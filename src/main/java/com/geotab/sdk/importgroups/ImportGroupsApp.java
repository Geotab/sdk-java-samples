package com.geotab.sdk.importgroups;

import static com.geotab.http.invoker.ServerInvoker.DEFAULT_TIMEOUT;

import com.geotab.api.GeotabApi;
import com.geotab.http.exception.DbUnavailableException;
import com.geotab.http.exception.InvalidUserException;
import com.geotab.http.request.AuthenticatedRequest;
import com.geotab.http.request.param.EntityParameters;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.http.response.GroupListResponse;
import com.geotab.http.response.IdResponse;
import com.geotab.model.Id;
import com.geotab.model.entity.group.CompanyGroup;
import com.geotab.model.entity.group.Group;
import com.geotab.model.login.Credentials;
import com.geotab.model.login.LoginResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImportGroupsApp {

  public static void main(String[] args) {
    if (args.length != 5) {
      System.out.println("Command line parameters:");
      System.out.println(
          "java -cp 'sdk-java-samples-1.0-SNAPSHOT.jar;./lib/*'"
              + " com.geotab.sdk.importgroups.ImportGroupsApp"
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
    List<CsvGroupEntry> groupEntries = loadGroupsFromCsv(filePath);

    // Create the Geotab API object used to make calls to the server
    // Note: server name should be the generic server as DBs can be moved without notice.
    // For example; use "my.geotab.com" rather than "my3.geotab.com".
    try (GeotabApi api = new GeotabApi(credentials, server, DEFAULT_TIMEOUT)) {

      // Authenticate user
      authenticate(api);

      // Start import
      importGroups(api, groupEntries);
    }
  }

  /**
   * Loads a csv file and processes rows into a collection of {@link CsvGroupEntry}.
   *
   * @param filePath The csv file name
   * @return A collection of {@link CsvGroupEntry}.
   */
  private static List<CsvGroupEntry> loadGroupsFromCsv(String filePath) {
    log.debug("Loading CSV {} ...", filePath);

    try (Stream<String> rows = Files.lines(Paths.get(filePath))) {
      return rows
          .filter(row -> row != null && !row.startsWith("#"))
          .map(row -> {
            String[] columns = row.split(",");
            return CsvGroupEntry.builder()
                .parentGroupName(columns[0])
                .groupName(columns[1])
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

  private static void importGroups(GeotabApi api, List<CsvGroupEntry> groupEntries) {
    log.debug("Start importing groups ...");

    try {
      List<Group> existingGroups = getExistingGroups(api);

      // Add groups
      for (CsvGroupEntry groupEntry : groupEntries) {
        // Assigning the parent node. //

        // When adding a node it, must have a parent.
        Group parentGroup;
        String parentGroupName = groupEntry.getParentGroupName();

        // If there is no parent node name or if the parent node's name matches organization
        // or entire organization create a new CompanyGroup object.
        if (parentGroupName == null || parentGroupName.isEmpty()
            || "organization".equals(parentGroupName.toLowerCase())
            || "entire organization".equals(parentGroupName.toLowerCase())) {
          parentGroup = new CompanyGroup();
        } else {
          Optional<Group> parentGroupFromServer = findGroup(existingGroups, parentGroupName);

          // This will need re-loading when there is a node we previously added
          // that we now want to assign children to.
          if (!parentGroupFromServer.isPresent()) {
            existingGroups = getExistingGroups(api);
          }

          parentGroupFromServer = findGroup(existingGroups, parentGroupName);
          // Check for non-organization Group in the dictionary of nodes that exist in the system.
          if (!parentGroupFromServer.isPresent()) {
            log.info("Non-existent parent Group: {}", parentGroupName);
            continue;
          }

          parentGroup = parentGroupFromServer.get();
        }

        // If the parent is null then we cannot add the Group.
        // So we write to the console and try to add the next node.
        if (parentGroup == null) {
          log.info("No parent for Group {}", groupEntry.getGroupName());
          continue;
        }

        // Adding the new node //

        // If a node exists with this name we wont add it and try to add the next node.
        Optional<Group> groupFromServer = findGroup(existingGroups, groupEntry.getGroupName());
        if (groupFromServer.isPresent()) {
          log.info("A group with the name '{}' already exists, please change this group name.",
              groupEntry.getGroupName());
          continue;
        }

        try {
          // Create the group object.
          Group newGroup = Group.builder()
              .name(groupEntry.getGroupName())
              .parent(parentGroup)
              .build();

          // Add the group.
          AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
              .method("Add")
              .params(EntityParameters.entityParamsBuilder()
                  .typeName("Group")
                  .entity(newGroup)
                  .build())
              .build();

          Optional<Id> response = api.call(request, IdResponse.class);

          if (response.isPresent()) {
            log.info("Group {} added with id {} .",
                groupEntry.getGroupName(), response.get().getId());
          } else {
            log.warn("Group {} not added; no id returned", groupEntry.getGroupName());
          }
        } catch (Exception exception) {
          // Catch and display any error that occur when adding the group
          log.error("Failed to import group {}", groupEntry.getGroupName(), exception);
        }

      }

      log.info("Groups imported.");
    } catch (Exception exception) {
      log.error("Failed to get import groups", exception);
      System.exit(1);
    }

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
      if (group.getName().trim().equalsIgnoreCase(groupName.trim())) {
        return Optional.of(group);
      }
    }

    return Optional.empty();
  }
}
