package com.geotab.sdk.getlogs;

import static com.geotab.api.DataStore.DeviceEntity;
import static com.geotab.api.DataStore.LogRecordEntity;
import static com.geotab.api.WebMethods.GetAddresses;
import static com.geotab.http.invoker.ServerInvoker.DEFAULT_TIMEOUT;
import static com.geotab.util.DateTimeUtil.nowUtcLocalDateTime;
import static java.lang.System.out;
import static java.util.Collections.singletonList;

import com.geotab.api.Api;
import com.geotab.api.Api.MultiCallBuilder;
import com.geotab.api.GeotabApi;
import com.geotab.http.request.param.GetAddressesParameters;
import com.geotab.model.ReverseGeocodeAddress;
import com.geotab.model.coordinate.Coordinate;
import com.geotab.model.entity.device.Device;
import com.geotab.model.entity.logrecord.LogRecord;
import com.geotab.model.search.DeviceSearch;
import com.geotab.model.search.LogRecordSearch;
import com.geotab.sdk.Util.Arg;
import com.geotab.sdk.Util.Cmd;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This is a Geotab API example of downloading a device's logs. Steps:
 * <ol>
 * <li>Search for multiple devices or one by its serial number.
 * <li>Get logs associated with all devices for a given time period.
 * </ol>
 *
 * <p>A complete Geotab API object and method reference is available at the Geotab Developer page.
 */
public class GetLogsApp {

  public static void main(String[] args) throws Exception {
    Cmd cmd = new Cmd(GetLogsApp.class, new Arg("serialNumber", false, "Serial number of the device"));
    String serialNumber = cmd.get("serialNumber");

    try (Api api = new GeotabApi(cmd.credentials, cmd.server, DEFAULT_TIMEOUT)) {
      // Get all devices or, if SN is available, only one device by serial number
      List<Device> devices = api.callGet(DeviceEntity, DeviceSearch.builder()
          .serialNumber(serialNumber).build(), 10).orElseThrow();

      // Get logs for all (or one SN) devices
      LocalDateTime toDate = nowUtcLocalDateTime();
      LocalDateTime fromDate = toDate.minusDays(7);
      MultiCallBuilder call = api.buildMultiCall();
      Map<Device, Supplier<List<LogRecord>>> result = new HashMap<>();
      for (Device d : devices) {
        result.put(d, call.callGet(LogRecordEntity, LogRecordSearch.builder()
            .deviceSearch(d.getId()).fromDate(fromDate).toDate(toDate).build()));
      }
      call.execute(); // if succeeds, each supplier will contain the corresponding result

      // Print last week coordinates for each device
      result.forEach((device, logs) -> {
        for (LogRecord r : logs.get()) {
          List<Coordinate> coordinates = singletonList(r.toSimpleCoordinate());
          GetAddressesParameters parameters = GetAddressesParameters.builder().coordinates(coordinates).build();
          var addresses = api.callMethod(GetAddresses, parameters);
          ReverseGeocodeAddress address = addresses.flatMap(o -> o.stream().findFirst()).orElseThrow();
          out.format("%s Date: %s Lat: %s Lon: %s Address: %s%n", device.getSerialNumber(), r.getDateTime(),
              r.getLatitude(), r.getLongitude(), address.getFormattedAddress());
        }
      });
    }
  }
}
