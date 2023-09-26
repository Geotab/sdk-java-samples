package com.geotab.sdk.importdevices;

import static com.geotab.http.invoker.ServerInvoker.DEFAULT_TIMEOUT;
import static java.util.Optional.ofNullable;

import com.geotab.api.Api;
import com.geotab.api.GeotabApi;
import com.geotab.http.exception.DbUnavailableException;
import com.geotab.http.exception.InvalidUserException;
import com.geotab.http.request.param.EntityParameters;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.model.Id;
import com.geotab.model.entity.device.Device;
import com.geotab.model.entity.group.CompanyGroup;
import com.geotab.model.entity.group.Group;
import com.geotab.model.entity.group.RootGroup;
import com.geotab.model.entity.user.User;
import com.geotab.model.entity.worktime.WorkTimeStandardHours;
import com.geotab.model.login.LoginResult;
import com.geotab.model.search.UserSearch;
import com.geotab.sdk.Util.Arg;
import com.geotab.sdk.Util.Cmd;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportDevicesApp {

  private static final Logger log = LoggerFactory.getLogger(ImportDevicesApp.class);

  public static void main(String[] args) throws Exception {
    // Process command line arguments
    Cmd cmd = new Cmd(ImportDevicesApp.class, new Arg("filePath", true, "Location of the CSV file to import"));
    String filePath = cmd.get("filePath");

    // load CSV
    List<CsvDeviceEntry> deviceEntries = loadDevicesFromCsv(filePath);

    // Create the Geotab API object used to make calls to the server
    try (Api api = new GeotabApi(cmd.credentials, cmd.server, DEFAULT_TIMEOUT)) {

      // Authenticate user
      authenticate(api);

      // Get user
      User apiUser = getApiUser(api, cmd.credentials.getUserName());

      // Start import
      importDevices(api, apiUser, deviceEntries);
    }
  }

  /**
   * Loads a csv file and processes rows into a collection of {@link CsvDeviceEntry}.
   *
   * @param filePath The csv file name
   * @return A collection of {@link CsvDeviceEntry}.
   */
  private static List<CsvDeviceEntry> loadDevicesFromCsv(String filePath) {
    log.debug("Loading CSV {}…", filePath);

    try (Stream<String> rows = Files.lines(Paths.get(filePath))) {
      return rows
          .filter(row -> row != null && !row.startsWith("#"))
          .map(row -> {
            String[] columns = row.split(",");
            CsvDeviceEntry out = new CsvDeviceEntry();
            out.description = columns[0];
            out.serialNumber = columns[1];
            out.nodeName = columns.length > 2 ? columns[2] : "";
            out.vin = columns.length > 3 ? columns[3] : "";
            return out;
          })
          .collect(Collectors.toList());
    } catch (Exception exception) {
      log.error("Failed to load csv file {} : ", filePath, exception);
      System.exit(1);
    }

    return new ArrayList<>();
  }

  private static LoginResult authenticate(Api api) {
    log.debug("Authenticating…");

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

  private static User getApiUser(Api api, String username) {
    log.debug("Getting user {}…", username);

    User apiUser = null;
    try {
      Optional<List<User>> users = api.callGet(SearchParameters.searchParamsBuilder()
          .search(UserSearch.builder().name(username).build()).typeName("User").resultsLimit(1).build(), User.class);
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

  private static void importDevices(Api api, User apiUser,
      List<CsvDeviceEntry> deviceEntries) {
    log.debug("Start importing devices…");

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
        String[] groupNames = ofNullable(deviceEntry.nodeName).orElse("").split("\\|");

        // If there are no nodes for the device specified in the .csv we will try to assign to Org
        if (hasOrgGroupScope && ofNullable(deviceEntry.nodeName).orElse("").isEmpty()) {
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
                  deviceEntry.description, groupName);
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
                .equals(deviceEntry.serialNumber.replace("-", "")));
        if (deviceExists) {
          log.warn("Device already exists - {} . Ignoring it.", deviceEntry.description);
          continue;
        }

        try {
          // Create the device object.
          Device newDevice = Device.fromSerialNumber(deviceEntry.serialNumber);
          newDevice.setSerialNumber(deviceEntry.serialNumber.replace("-", ""));
          newDevice.populateDefaults();
          newDevice.setName(deviceEntry.description);
          newDevice.setGroups(deviceGroups);
          newDevice.setWorkTime(new WorkTimeStandardHours());

          // Add the device
          Optional<Id> response = api.callAdd(EntityParameters.entityParamsBuilder()
              .typeName("Device").entity(newDevice).build());

          if (response.isPresent()) {
            log.info("Device {} added with id {} .",
                deviceEntry.description, response.get().getId());
          } else {
            log.warn("Device {} not added; no id returned", deviceEntry.description);
          }
        } catch (Exception exception) {
          // Catch and display any error that occur when adding the device
          log.error("Failed to import device {}", deviceEntry.description, exception);
        }

      }

      log.info("Devices imported.");
    } catch (Exception exception) {
      log.error("Failed to get import devices", exception);
      System.exit(1);
    }
  }

  private static List<Device> getExistingDevices(Api api) {
    log.debug("Get existing devices…");
    try {
      return api.callGet(SearchParameters.searchParamsBuilder()
          .typeName("Device").build(), Device.class).orElse(new ArrayList<>());
    } catch (Exception exception) {
      log.error("Failed to get existing devices ", exception);
      System.exit(1);
    }

    return new ArrayList<>();
  }

  private static List<Group> getExistingGroups(Api api) {
    log.debug("Get existing groups…");
    try {
      return api.callGet(SearchParameters.searchParamsBuilder()
          .typeName("Group").build(), Group.class).orElse(new ArrayList<>());
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
