package com.geotab.sdk.importdevices;

import static com.geotab.plain.Entities.DeviceEntity;
import static com.geotab.plain.Entities.GroupEntity;
import static com.geotab.plain.Entities.UserEntity;
import static com.geotab.util.Util.apply;
import static java.util.Optional.ofNullable;

import com.geotab.api.Api;
import com.geotab.http.exception.DbUnavailableException;
import com.geotab.http.exception.InvalidUserException;
import com.geotab.model.Id;
import com.geotab.model.login.LoginResult;
import com.geotab.plain.objectmodel.Device;
import com.geotab.plain.objectmodel.Group;
import com.geotab.plain.objectmodel.User;
import com.geotab.plain.objectmodel.UserSearch;
import com.geotab.plain.objectmodel.WorkTime;
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
    try (Api api = cmd.newApi()) {

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
      return rows.filter(row -> row != null && !row.startsWith("#"))
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
      var users = api.callGet(UserEntity, apply(new UserSearch(), s -> s.name = username), 1);
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

  private static void importDevices(Api api, User apiUser, List<CsvDeviceEntry> deviceEntries) {
    log.debug("Start importing devices…");

    try {
      List<Device> existingDevices = getExistingDevices(api);
      List<Group> existingGroups = getExistingGroups(api);

      // We only want to be able to assign Org Group if the API user has this in their scope.
      boolean hasOrgGroupScope =
        apiUser.companyGroups != null
          && apiUser.companyGroups.stream().anyMatch(Group::isSystemEntity);

      // Add devices
      for (CsvDeviceEntry deviceEntry : deviceEntries) {
        boolean deviceRejected = false;
        List<Group> deviceGroups = new ArrayList<>();

        // A devices and nodes have a many to many relationship.
        // In the .csv file if a device belongs to multiple nodes we separate with a pipe character.
        String[] groupNames = ofNullable(deviceEntry.nodeName).orElse("").split("\\|");

        // If there are no nodes for the device specified in the .csv we will try to assign to Org
        if (hasOrgGroupScope && ofNullable(deviceEntry.nodeName).orElse("").isEmpty()) {
          deviceGroups.add(Group.fromString("GroupCompanyId"));
        }

        // Iterate through the group names and try to assign each group
        // to the device looking it up from the allNodes collection.
        for (String groupName : groupNames) {
          // Organization group.
          if (hasOrgGroupScope && "organization".equals(groupName.trim().toLowerCase())
            || "entire organization".equals(groupName.trim().toLowerCase())) {
            deviceGroups.add(Group.fromString("GroupCompanyId"));
          } else {
            // Get the group from allNodes
            Optional<Group> existingGroup = findGroup(existingGroups, groupName);

            if (!existingGroup.isPresent()) {
              log.warn("Device Rejected - {} . Group {} does not exist.", deviceEntry.description, groupName);
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
        String cleanSerial = deviceEntry.serialNumber.replace("-", "");
        boolean deviceExists = existingDevices.stream()
          .anyMatch(device -> device.serialNumber != null && device.serialNumber.equals(cleanSerial));
        if (deviceExists) {
          log.warn("Device already exists - {} . Ignoring it.", deviceEntry.description);
          continue;
        }

        try {
          // Create the device object.
          Device newDevice =
            apply(
              new Device(),
              d -> {
                d.setName(deviceEntry.description);
                d.serialNumber = cleanSerial;
                d.groups = deviceGroups;
                d.workTime = WorkTime.fromString("WorkTimeStandardHoursId");
              });

          // Add the device
          Optional<Id> response = api.callAdd(DeviceEntity, newDevice);

          if (response.isPresent()) {
            log.info("Device {} added with id {} .", deviceEntry.description, response.get().getId());
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
      return api.callGet(DeviceEntity, null, null).orElse(new ArrayList<>());
    } catch (Exception exception) {
      log.error("Failed to get existing devices ", exception);
      System.exit(1);
    }

    return new ArrayList<>();
  }

  private static List<Group> getExistingGroups(Api api) {
    log.debug("Get existing groups…");
    try {
      return api.callGet(GroupEntity, null, null).orElse(new ArrayList<>());
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
