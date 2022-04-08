package com.geotab.sdk.getlogs;

import static com.geotab.http.invoker.ServerInvoker.DEFAULT_TIMEOUT;
import static com.geotab.util.DateTimeUtil.nowUtcLocalDateTime;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.System.out;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.geotab.api.Api.MultiCallBuilder;
import com.geotab.api.GeotabApi;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.model.entity.device.Device;
import com.geotab.model.entity.logrecord.LogRecord;
import com.geotab.model.search.DeviceSearch;
import com.geotab.model.search.LogRecordSearch;
import com.geotab.sdk.Util.Arg;
import com.geotab.sdk.Util.Cmd;
import java.time.LocalDateTime;
import java.util.Collections;
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

  public static void main(String[] args) {
    Cmd cmd = new Cmd(GetLogsApp.class, new Arg("serialNumber", false, "Serial number of the device"));
    String serialNumber = cmd.get("serialNumber");

    try (GeotabApi api = new GeotabApi(cmd.credentials, cmd.server, DEFAULT_TIMEOUT)) {
      // Get all devices or, if SN is available, only one device by serial number
      List<Device> devices = api.callGet(SearchParameters.searchParamsBuilder()
          .resultsLimit(10).typeName("Device")
          .search(isNullOrEmpty(serialNumber) ? null : DeviceSearch.builder().serialNumber(serialNumber).build())
          .build(), Device.class).orElse(Collections.emptyList());

      // Get logs for all (or one SN) devices
      LocalDateTime toDate = nowUtcLocalDateTime();
      LocalDateTime fromDate = toDate.minusDays(7);
      MultiCallBuilder multiCall = api.buildMultiCall();
      Map<Device, Supplier<List<LogRecord>>> result = devices.stream()
          .collect(toMap(identity(), o -> multiCall.callGet(SearchParameters.searchParamsBuilder().typeName("LogRecord")
              .search(LogRecordSearch.builder().deviceSearch(o.getId()).fromDate(fromDate).toDate(toDate).build())
              .build(), LogRecord.class)));
      multiCall.execute(); // if succeeds, each supplier will contain the corresponding result

      // Print last week coordinates for each device
      result.forEach((device, logs) -> {
        for (LogRecord r : logs.get()) {
          out.format("%s Date: %s Lat: %s Lon: %s%n",
              device.getSerialNumber(), r.getDateTime(), r.getLatitude(), r.getLongitude());
        }
      });
    }
  }
}
