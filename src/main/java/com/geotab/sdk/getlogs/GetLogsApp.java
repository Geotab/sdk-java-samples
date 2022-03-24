package com.geotab.sdk.getlogs;

import static com.geotab.http.invoker.ServerInvoker.DEFAULT_TIMEOUT;
import static com.geotab.http.request.MultiCallRequest.multiCallRequestBuilder;
import static com.geotab.util.DateTimeUtil.nowUtcLocalDateTime;

import com.geotab.api.GeotabApi;
import com.geotab.http.exception.DbUnavailableException;
import com.geotab.http.exception.InvalidUserException;
import com.geotab.http.request.AuthenticatedRequest;
import com.geotab.http.request.BaseRequest;
import com.geotab.http.request.param.MultiCallParameters;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.http.response.BaseResponse;
import com.geotab.http.response.DeviceListResponse;
import com.geotab.model.entity.device.Device;
import com.geotab.model.entity.logrecord.LogRecord;
import com.geotab.model.login.Credentials;
import com.geotab.model.login.LoginResult;
import com.geotab.model.search.DeviceSearch;
import com.geotab.model.search.LogRecordSearch;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * This is a Geotab API  example of downloading a device's logs.
 *
 * <p>Steps:
 *
 * <p>1) Authenticate a user via login, password, database and server using the Geotab
 * API object.
 *
 * <p>2) Search for a device by its serial number.
 *
 * <p>3) Get logs associated with the device for a given time period.
 *
 * <p>A complete Geotab API object and method reference is available at the Geotab Developer page.
 */
@Slf4j
public class GetLogsApp {

  public static void main(String[] args) {
    if (args.length < 4) {
      System.out.println("Command line parameters:");
      System.out.println(
          "java -cp 'sdk-java-samples-1.0-SNAPSHOT.jar;./lib/*' com.geotab.sdk.getlogs.GetLogsApp"
              + " 'server' 'database' 'username' 'password' 'serialNumber'");
      System.out.println("server             - The server name (Example: my.geotab.com)");
      System.out.println("database           - The database name (Example: G560)");
      System.out.println("username           - The Geotab user name");
      System.out.println("password           - The Geotab password");
      System.out.println("serialNumber       - Serial number of the device.");
      System.exit(1);
    }

    // Process command line arguments
    String server = args[0];
    String database = args[1];
    String username = args[2];
    String password = args[3];
    String serialNumber = args.length > 4 ? args[4] : "";

    Credentials credentials = Credentials.builder()
        .database(database)
        .password(password)
        .userName(username)
        .build();

    // Create the Geotab API object used to make calls to the server
    // Note: server name should be the generic server as DBs can be moved without notice.
    // For example; use "my.geotab.com" rather than "my3.geotab.com".
    try (GeotabApi api = new GeotabApi(credentials, server, DEFAULT_TIMEOUT)) {

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

      // Get device by serialNumber
      List<Device> devices = null;
      try {
        AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
            .method("Get")
            .params(SearchParameters.searchParamsBuilder()
                .credentials(loginResult.getCredentials()).resultsLimit(10).typeName("Device")
                .search(serialNumber.isEmpty() ? null : DeviceSearch.builder().serialNumber(serialNumber).build())
                .build())
            .build();

        Optional<List<Device>> deviceListResponse = api.call(request, DeviceListResponse.class);
        if (!deviceListResponse.isPresent() || deviceListResponse.get().isEmpty()) {
          log.error("No device found");
          System.exit(1);
        }

        devices = deviceListResponse.get();
      } catch (Exception exception) {
        log.error("Failed to get devices: ", exception);
        System.exit(1);
      }

      // Get logs for the Device
      try {
        LocalDateTime toDate = nowUtcLocalDateTime();
        LocalDateTime fromDate = toDate.minusDays(7);

        List<BaseRequest<?>> requests = new ArrayList<>();

        for (Device device : devices) {
          requests.add(AuthenticatedRequest.authRequestBuilder()
              .method("Get")
              .params(SearchParameters.searchParamsBuilder()
                  .credentials(loginResult.getCredentials())
                  .typeName("LogRecord")
                  .search(LogRecordSearch.builder()
                      .deviceSearch(device.getId())
                      .fromDate(fromDate)
                      .toDate(toDate)
                      .build())
                  .build())
              .build());
        }

        class DummyClassToShareTheType extends BaseResponse<List<List<LogRecord>>> {
        }

        Optional<List<List<LogRecord>>> result = api.multiCall(multiCallRequestBuilder()
            .params(MultiCallParameters.multiCallParamsBuilder().calls(requests).build())
            .build(), DummyClassToShareTheType.class);

        if (!result.isPresent() || result.get().isEmpty()) {
          log.info("No Logs Found");
        } else {
          List<List<LogRecord>> get = result.get();
          for (int i = 0; i < get.size(); i++) {
            Device device = devices.get(i);
            List<LogRecord> logs = get.get(i);
            // We will display the Lat, Lon, and Date of each Log as a row.
            for (LogRecord logRecord : logs) {
              System.out.println(device.getSerialNumber()
                  + " Lat: " + logRecord.getLatitude()
                  + " Lon: " + logRecord.getLongitude()
                  + " Date: " + logRecord.getDateTime());
            }

          }
        }
      } catch (Exception exception) {
        log.error("Failed to get logs: ", exception);
      }
    }
  }
}
