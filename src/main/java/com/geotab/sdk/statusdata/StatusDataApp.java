package com.geotab.sdk.statusdata;

import static com.geotab.http.invoker.ServerInvoker.DEFAULT_TIMEOUT;
import static com.geotab.http.request.param.SearchParameters.searchParamsBuilder;
import static java.lang.System.out;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.geotab.api.GeotabApi;
import com.geotab.model.entity.NameEntity;
import com.geotab.model.entity.device.Device;
import com.geotab.model.entity.diagnostic.Diagnostic;
import com.geotab.model.entity.statusdata.StatusData;
import com.geotab.model.search.DeviceSearch;
import com.geotab.model.search.StatusDataSearch;
import com.geotab.sdk.Util.Arg;
import com.geotab.sdk.Util.Cmd;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;

public class StatusDataApp {

  public static void main(String[] args) throws Exception {
    var cmd = new Cmd(StatusDataApp.class, new Arg("serialNumber", true, "Serial number of the device"));
    var serialNumber = cmd.get("serialNumber");

    try (var api = new GeotabApi(cmd.credentials, cmd.server, DEFAULT_TIMEOUT)) {
      var device = api.callGet(searchParamsBuilder().typeName("Device")
          .search(DeviceSearch.builder().serialNumber(serialNumber).build())
          .build(), Device.class).orElse(Collections.emptyList()).get(0);
      out.println("Loading statusData for vehicle " + device.getName() + "[" + device.getId().getId() + "]…");

      var mcb = api.buildMultiCall();
      var deviceS = DeviceSearch.builder().id(device.getId().getId()).build();
      var data = mcb.callGet(searchParamsBuilder().typeName("StatusData")
          .search(StatusDataSearch.builder().deviceSearch(deviceS).build()).build(), StatusData.class);
      var diagnostics = mcb.callGet(searchParamsBuilder().typeName("Diagnostic").build(), Diagnostic.class);
      mcb.execute();

      var diagnosticsById = diagnostics.get().stream().collect(toMap(o -> o.getId().getId(), NameEntity::getName));
      var enhancedData = data.get().stream().map(statusData -> {
        var diagnosticId = statusData.getDiagnostic().getId().getId();
        var diagnosticName = diagnosticsById.getOrDefault(diagnosticId, diagnosticId);
        return new SimpleEntry<>(diagnosticName, statusData.getDateTime() + "=" + statusData.getData());
      }).collect(toList());

      for (var datum : enhancedData) {
        out.println(datum);
      }
    }
  }
}
