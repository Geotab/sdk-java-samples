package com.geotab.sdk.getlogs;

import static com.geotab.plain.Entities.DeviceEntity;
import static com.geotab.util.DateTimeUtil.nowUtcLocalDateTime;
import static com.geotab.util.Util.apply;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.System.out;

import com.geotab.api.Api;
import com.geotab.plain.Entities;
import com.geotab.plain.WebMethods;
import com.geotab.plain.objectmodel.Coordinate;
import com.geotab.plain.objectmodel.Device;
import com.geotab.plain.objectmodel.DeviceSearch;
import com.geotab.plain.objectmodel.LogRecord;
import com.geotab.plain.objectmodel.LogRecordSearch;
import com.geotab.plain.parameters.GetAddressesParameters;
import com.geotab.sdk.Util.Arg;
import com.geotab.sdk.Util.Cmd;
import java.util.HashMap;
import java.util.List;
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
    var cmd = new Cmd(GetLogsApp.class, new Arg("serialNumber", false, "Serial number of the device"));
    var serialNumber = cmd.get("serialNumber");

    try (Api api = cmd.newApi()) {
      // Get 10 devices or, if SN is available, only one device by serial number
      var devices = api.callGet(DeviceEntity, apply(new DeviceSearch(), s -> s.serialNumber = serialNumber), 100).orElseThrow()
          .stream().filter(d -> isNullOrEmpty(d.serialNumber) || "000-000-0000".equals(d.serialNumber))
          .toList();

      // Get logs for all (or one SN) devices
      var toDate = nowUtcLocalDateTime();
      var fromDate = toDate.minusDays(7);
      var call = api.buildMultiCall();
      var result = new HashMap<Device, Supplier<List<LogRecord>>>();
      for (var d : devices) {
        out.format("🚗Getting logs for %s [serialNumber=%s, name=%s, deviceType=%s]…%n",
            d.getId(), d.serialNumber, d.getName(), d.deviceType);
        result.put(d, call.callGet(Entities.LogRecordEntity, apply(new LogRecordSearch(), s -> {
          s.deviceSearch = apply(new com.geotab.plain.objectmodel.DeviceSearch(), ds -> ds.setId(d.getId().getId()));
          s.fromDate = fromDate;
          s.toDate = toDate;
        })));
      }
      call.execute(); // if succeeds, each supplier will contain the corresponding result
      for (var entry : result.entrySet()) {
        for (var device : result.keySet()) {
          out.format("🟢Logs for %s [serialNumber=%s, name=%s, deviceType=%s]: %s logs%n", device.serialNumber,
              device.getId(), device.getName(), device.deviceType, entry.getValue().get().size());
        }
      }

      // Print last week coordinates for each device
      for (var entry : result.entrySet()) {
        if (entry.getValue().get().isEmpty()) continue;
        var lastLog = entry.getValue().get().getLast();
        var coordinate = apply(new Coordinate(), c -> {
          c.y = lastLog.latitude;
          c.x = lastLog.longitude;
        });
        var parameters = apply(new GetAddressesParameters(), p -> p.coordinates = List.of(coordinate));
        var addresses = api.call(WebMethods.GetAddresses, parameters);
        var address = addresses.flatMap(o -> o.stream().findFirst()).orElseThrow();
        out.format("📌Address for %s [date=%s, lat=%s, lon=%s]: %s%n", entry.getKey().getId(), lastLog.dateTime,
            lastLog.latitude, lastLog.longitude, address.formattedAddress);
      }
    }
  }
}
