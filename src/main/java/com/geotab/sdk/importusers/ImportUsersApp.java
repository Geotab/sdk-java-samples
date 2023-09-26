package com.geotab.sdk.importusers;

import static com.geotab.http.invoker.ServerInvoker.DEFAULT_TIMEOUT;
import static com.geotab.model.entity.user.UserAuthenticationType.BASIC_AUTHENTICATION;

import com.geotab.api.Api;
import com.geotab.api.GeotabApi;
import com.geotab.http.exception.DbUnavailableException;
import com.geotab.http.exception.InvalidUserException;
import com.geotab.http.request.param.EntityParameters;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.model.Id;
import com.geotab.model.entity.group.Group;
import com.geotab.model.entity.group.SecurityGroup;
import com.geotab.model.entity.user.User;
import com.geotab.model.login.LoginResult;
import com.geotab.model.search.GroupSearch;
import com.geotab.sdk.Util.Arg;
import com.geotab.sdk.Util.Cmd;
import com.google.common.collect.Iterables;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportUsersApp {

  private static final Logger log = LoggerFactory.getLogger(ImportUsersApp.class);

  public static void main(String[] args) throws Exception {
    // Process command line arguments
    Cmd cmd = new Cmd(ImportUsersApp.class, new Arg("filePath", true, "Location of the CSV file to import"));
    String filePath = cmd.get("filePath");

    // load CSV
    List<UserDetails> userEntries = loadUsersFromCsv(filePath);

    // Create the Geotab API object used to make calls to the server
    try (Api api = new GeotabApi(cmd.credentials, cmd.server, DEFAULT_TIMEOUT)) {

      // Authenticate user
      authenticate(api);

      // Start import
      importUsers(api, userEntries);
    }
  }

  /**
   * Loads a csv file and processes rows into a collection of {@link UserDetails}.
   *
   * @param filePath The csv file name
   * @return A collection of {@link UserDetails}.
   */
  private static List<UserDetails> loadUsersFromCsv(String filePath) {
    log.debug("Loading CSV {}…", filePath);

    LocalDateTime minDate = LocalDateTime.of(1986, 1, 1, 0, 0, 0, 0);
    LocalDateTime maxDate = LocalDateTime.of(2050, 1, 1, 0, 0, 0, 0);

    try (Stream<String> rows = Files.lines(Paths.get(filePath))) {
      return rows
          .filter(row -> row != null && !row.startsWith("#"))
          .map(row -> {
            String[] columns = row.split(",");
            String userName = columns[0].trim();
            String password = columns[1].trim();
            String organizationNodes = columns[2].trim();
            String securityNodes = columns[3].trim();
            String firstName = columns[4].trim();
            String lastName = columns[5].trim();

            User user = User.userBuilder()
                .name(userName)
                .firstName(firstName)
                .lastName(lastName)
                .password(password)
                .userAuthenticationType(BASIC_AUTHENTICATION)
                .activeFrom(minDate)
                .activeTo(maxDate)
                .privateUserGroups(new ArrayList<>())
                .timeZoneId("America/Los_Angeles")
                .isDriver(false)
                .isEmailReportEnabled(true)
                .build();

            UserDetails out = new UserDetails();
            out.user = user;
            out.organizationNodeNames = organizationNodes;
            out.securityNodeName = securityNodes;
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

  private static void importUsers(Api api, List<UserDetails> userEntries) {
    log.debug("Start importing users…");

    try {

      List<User> existingUsers = getExistingUsers(api);
      List<Group> existingGroups = getExistingGroups(api);
      List<Group> securityGroups = getSecurityGroups(api);

      for (UserDetails userDetails : userEntries) {
        // Add groups to user
        User user = userDetails.user;
        user.setCompanyGroups(getOrganizationGroups(userDetails.organizationNodeNames.split("\\|"), existingGroups));
        user.setSecurityGroups(filterSecurityGroupsByName(userDetails.securityNodeName, securityGroups));
        if (isUserValid(user, existingUsers)) {
          try {
            // Add the user
            Optional<Id> response = api.callAdd(EntityParameters.entityParamsBuilder()
                .typeName("User").entity(user).build());

            if (response.isPresent()) {
              log.info("User {} added with id {}", user.getName(), response.get().getId());
              user.setId(new Id(response.get().getId()));
              existingUsers.add(user);
            } else {
              log.warn("User {} not added; no id returned", user.getName());
            }
          } catch (Exception exception) {
            // Catch and display any error that occur when adding the user
            log.error("Failed to import user {}", user.getName(), exception);
          }
        }

      }

      log.info("Users imported.");
    } catch (Exception exception) {
      log.error("Failed to get import users", exception);
      System.exit(1);
    }

  }

  private static List<User> getExistingUsers(Api api) {
    log.debug("Get existing users…");
    try {
      return api.callGet(SearchParameters.searchParamsBuilder()
          .typeName("User").build(), User.class).orElse(new ArrayList<>());
    } catch (Exception exception) {
      log.error("Failed to get existing users ", exception);
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

  private static List<Group> getSecurityGroups(Api api) {
    log.debug("Get security groups…");
    try {
      return api.callGet(SearchParameters.searchParamsBuilder().typeName("Group")
              .search(GroupSearch.builder().id(SecurityGroup.SECURITY_GROUP_ID).build()).build(), Group.class)
          .orElse(new ArrayList<>());
    } catch (Exception exception) {
      log.error("Failed to get security groups ", exception);
      System.exit(1);
    }

    return new ArrayList<>();
  }

  /**
   * Searches a list of organization groups matching the names provided.
   */
  private static List<Group> getOrganizationGroups(String[] groupNames, List<Group> groups) {
    List<Group> organizationGroups = new ArrayList<>();
    for (String groupName : groupNames) {
      String name = groupName.trim().toLowerCase();
      if ("organization".equals(name) || "entire organization".equals(name)) {
        name = "**Org**";
      }
      for (Group group : groups) {
        if (group.getName().equalsIgnoreCase(name)) {
          organizationGroups.add(group);
          break;
        }
      }
    }
    return organizationGroups;
  }

  /**
   * Searches a list of security groups matching the names provided.
   */
  private static List<Group> filterSecurityGroupsByName(String name, List<Group> securityGroups) {
    List<Group> groups = new ArrayList<>();

    if (name != null && !name.isEmpty()) {
      name = name.trim().toLowerCase();
      if (name.equals("administrator") || name.equals("admin")) {
        name = "**EverythingSecurity**";
      }
      if (name.equals("superviser") || name.equals("supervisor")) {
        name = "**SupervisorSecurity**";
      }
      if (name.equals("view only") || name.equals("viewonly")) {
        name = "**ViewOnlySecurity**";
      }
      if (name.equals("nothing")) {
        name = "**NothingSecurity**";
      }
      for (Group securityGroup : securityGroups) {
        if (securityGroup.getName().equals(name)) {
          groups.add(securityGroup);
          break;
        }
      }
    }

    return groups;
  }

  /**
   * Validate a user has groups assigned and does not exist.
   */
  private static boolean isUserValid(User user, List<User> existingUsers) {
    if (Iterables.isEmpty(user.getCompanyGroups())) {
      log.warn("Invalid user: {}. Must have organization nodes.", user.getName());
      return false;
    }
    if (Iterables.isEmpty(user.getSecurityGroups())) {
      log.warn("Invalid user: {}. Must have security nodes.", user.getName());
      return false;
    }
    boolean userExists = existingUsers.stream()
        .anyMatch(existingUser -> existingUser.getName().equalsIgnoreCase(user.getName()));
    if (userExists) {
      log.warn("Invalid user: {}. Duplicate user.", user.getName());
      return false;
    }
    return true;
  }

}

