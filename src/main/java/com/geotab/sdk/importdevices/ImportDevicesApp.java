package com.geotab.sdk.importdevices;

import static com.geotab.http.invoker.ServerInvoker.DEFAULT_TIMEOUT;
import static java.util.Optional.ofNullable;

import com.geotab.api.GeotabApi;
import com.geotab.http.exception.DbUnavailableException;
import com.geotab.http.exception.InvalidUserException;
import com.geotab.http.request.AuthenticatedRequest;
import com.geotab.http.request.param.EntityParameters;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.http.response.DeviceListResponse;
import com.geotab.http.response.GroupListResponse;
import com.geotab.http.response.IdResponse;
import com.geotab.http.response.UserListResponse;
import com.geotab.model.Id;
import com.geotab.model.entity.device.Device;
import com.geotab.model.entity.group.CompanyGroup;
import com.geotab.model.entity.group.Group;
import com.geotab.model.entity.group.RootGroup;
import com.geotab.model.entity.user.User;
import com.geotab.model.entity.worktime.WorkTimeStandardHours;
import com.geotab.model.login.Credentials;
import com.geotab.model.login.LoginResult;
import com.geotab.model.search.UserSearch;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImportDevicesApp {

  public static void main(String[] args) throws Exception {
    try {
      if (args.length != 5) {
        System.out.println("Command line parameters:");
        System.out.println(
            "java -cp 'sdk-java-samples-1.0-SNAPSHOT.jar;./lib/*'"
                + " com.geotab.sdk.importdevices.ImportDevicesApp"
                + " 'my.geotab.com' 'database' 'user@email.com' 'password' 'inputFileLocation'");
        System.out.println("server             - The server name (Example: my.geotab.com)");
        System.out.println("database           - The database name (Example: G560)");
        System.out.println("username           - The Geotab user name");
        System.out.println("password           - The Geotab password");
        System.out.println("inputFileLocation  - Location of the CSV file to import.");
        System.exit(1);
      }

      // Process command line arguments
      String server = args[0];
      String database = args[1];
      String username = args[2];
      String password = args[3];
      String filePath = args[4];

      Credentials credentials = Credentials.builder()
          .database(database)
          .password(password)
          .userName(username)
          .build();

      // load CSV
      List<CsvDeviceEntry> deviceEntries = loadDevicesFromCsv(filePath);

      // Create the Geotab API object used to make calls to the server
      // Note: server name should be the generic server as DBs can be moved without notice.
      // For example; use "my.geotab.com" rather than "my3.geotab.com".
      try (GeotabApi api = new GeotabApi(credentials, server, DEFAULT_TIMEOUT)) {

        // Authenticate user
        authenticate(api);

        // Get user
        User apiUser = getApiUser(api, username);

        // Start import
        importDevices(api, apiUser, deviceEntries);
      }

    } catch (Exception exception) {
      // Show miscellaneous exceptions
      log.error("Unhandled exception: ", exception);
    } finally {
      log.info("Press Enter to exit...");
      System.in.read();
    }
  }

  /**
   * Loads a csv file and processes rows into a collection of {@link CsvDeviceEntry}.
   *
   * @param filePath The csv file name
   * @return A collection of {@link CsvDeviceEntry}.
   */
  private static List<CsvDeviceEntry> loadDevicesFromCsv(String filePath) {
    log.debug("Loading CSV {} ...", filePath);

    try (Stream<String> rows = Files.lines(Paths.get(filePath))) {
      return rows
          .filter(row -> row != null && !row.startsWith("#"))
          .map(row -> {
            String[] columns = row.split(",");
            return CsvDeviceEntry.builder()
                .description(columns[0])
                .serialNumber(columns[1])
                .nodeName(columns.length > 2 ? columns[2] : "")
                .vin(columns.length > 3 ? columns[3] : "")
                .build();
          })
          .collect(Collectors.toList());
    } catch (Exception exception) {
      log.error("Failed to load csv file {} : ", filePath, exception);
      System.exit(1);
    }

    return new ArrayList<>();
  }

  private static LoginResult authenticate(GeotabApi api) {
    log.debug("Authenticating ...");

    LoginResult loginResult = null;

    // Authenticate user
    try {
      loginResult = api.authenticate();
      log.info("Successfully Authenticated");
    } catch (InvalidUserException exception) {
      log.error("Invalid user: ", exception);
      System.exit(1);
    } catch (DbUnavailableException exception) {
      log.error("Database unavailable: ", exception);
      System.exit(1);
    } catch (Exception exception) {
      log.error("Failed to authenticate user: ", exception);
      System.exit(1);
    }

    return loginResult;
  }

  private static User getApiUser(GeotabApi api, String username) {
    log.debug("Getting user {} ...", username);

    User apiUser = null;
    try {
      AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
          .method("Get")
          .params(SearchParameters.searchParamsBuilder()
              .search(UserSearch.builder()
                  .name(username)
                  .build())
              .typeName("User")
              .resultsLimit(1)
              .build())
          .build();

      Optional<List<? extends User>> users = api.call(request, UserListResponse.class);
      if (!users.isPresent() || users.get().isEmpty()) {
        log.error("User {} not found", username);
        System.exit(1);
      }
      apiUser = users.get().get(0);
    } catch (Exception exception) {
      log.error("Failed to get user: {} ", username, exception);
      System.exit(1);
    }

    return apiUser;
  }

  private static void importDevices(GeotabApi api, User apiUser,
      List<CsvDeviceEntry> deviceEntries) {
    log.debug("Start importing devices ...");

    try {
      List<Device> existingDevices = getExistingDevices(api);
      List<Group> existingGroups = getExistingGroups(api);

      // We only want to be able to assign Org Group if the API user has this in their scope.
      boolean hasOrgGroupScope = apiUser.getCompanyGroups().stream()
          .anyMatch(group -> group instanceof CompanyGroup || group instanceof RootGroup);

      // Add devices
      for (CsvDeviceEntry deviceEntry : deviceEntries) {
        boolean deviceRejected = false;
        List<Group> deviceGroups = new ArrayList<>();

        // A devices and nodes have a many to many relationship.
        // In the .csv file if a device belongs to multiple nodes we separate with a pipe character.
        String[] groupNames = ofNullable(deviceEntry.getNodeName()).orElse("").split("\\|");

        // If there are no nodes for the device specified in the .csv we will try to assign to Org
        if (hasOrgGroupScope && ofNullable(deviceEntry.getNodeName()).orElse("").isEmpty()) {
          deviceGroups.add(new CompanyGroup());
        }

        // Iterate through the group names and try to assign each group
        // to the device looking it up from the allNodes collection.
        for (String groupName : groupNames) {
          // Organization group.
          if (hasOrgGroupScope
              && "organization".equals(groupName.trim().toLowerCase())
              || "entire organization".equals(groupName.trim().toLowerCase())) {
            deviceGroups.add(new CompanyGroup());
          } else {
            // Get the group from allNodes
            Optional<Group> existingGroup = findGroup(existingGroups, groupName);

            if (!existingGroup.isPresent()) {
              log.warn("Device Rejected - {} . Group {} does not exist.",
                  deviceEntry.getDescription(), groupName);
              deviceRejected = true;
              break;
            }

            // Add group to device nodes collection.
            deviceGroups.add(existingGroup.get());
          }
        }

        // If the device is rejected, move on the the next device row.
        if (deviceRejected) {
          continue;
        }

        // Check for an existing device.
        boolean deviceExists = existingDevices
            .stream()
            .anyMatch(device -> device.getSerialNumber()
                .equals(deviceEntry.getSerialNumber().replace("-", "")));
        if (deviceExists) {
          log.warn("Device already exists - {} . Ignoring it.", deviceEntry.getDescription());
          continue;
        }

        try {
          // Create the device object.
          Device newDevice = Device.fromSerialNumber(deviceEntry.getSerialNumber());
          newDevice.setSerialNumber(deviceEntry.getSerialNumber().replace("-", ""));
          newDevice.populateDefaults();
          newDevice.setName(deviceEntry.getDescription());
          newDevice.setGroups(deviceGroups);
          newDevice.setWorkTime(new WorkTimeStandardHours());

          // Add the device.
          AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
              .method("Add")
              .params(EntityParameters.entityParamsBuilder()
                  .typeName("Device")
                  .entity(newDevice)
                  .build())
              .build();

          Optional<Id> response = api.call(request, IdResponse.class);

          if (response.isPresent()) {
            log.info("Device {} added with id {} .",
                deviceEntry.getDescription(), response.get().getId());
          } else {
            log.warn("Device {} not added; no id returned", deviceEntry.getDescription());
          }
        } catch (Exception exception) {
          // Catch and display any error that occur when adding the device
          log.error("Failed to import device {}", deviceEntry.getDescription(), exception);
        }

      }

      log.info("Devices imported.");
    } catch (Exception exception) {
      log.error("Failed to get import devices", exception);
      System.exit(1);
    }

  }

  private static List<Device> getExistingDevices(GeotabApi api) {
    log.debug("Get existing devices ...");
    try {
      AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
          .method("Get")
          .params(SearchParameters.searchParamsBuilder()
              .typeName("Device")
              .build())
          .build();

      return api.call(request, DeviceListResponse.class).orElse(new ArrayList<>());
    } catch (Exception exception) {
      log.error("Failed to get existing devices ", exception);
      System.exit(1);
    }

    return new ArrayList<>();
  }

  private static List<Group> getExistingGroups(GeotabApi api) {
    log.debug("Get existing groups ...");
    try {
      AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
          .method("Get")
          .params(SearchParameters.searchParamsBuilder()
              .typeName("Group")
              .build())
          .build();

      return api.call(request, GroupListResponse.class).orElse(new ArrayList<>());
    } catch (Exception exception) {
      log.error("Failed to get existing groups ", exception);
      System.exit(1);
    }

    return new ArrayList<>();
  }

  private static Optional<Group> findGroup(List<Group> existingGroups, String groupName) {
    for (Group group : existingGroups) {
      if (group.getName().trim().equalsIgnoreCase(groupName)) {
        return Optional.of(group);
      }
    }

    return Optional.empty();
  }
}
