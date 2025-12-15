package com.geotab.sdk.maintenance;

import static com.geotab.plain.Entities.MaintenanceWorkOrderEntity;
import static com.geotab.plain.Entities.MaintenanceWorkOrderJobEntity;
import static com.geotab.plain.Entities.MaintenanceWorkRequestEntity;
import static com.geotab.util.Util.apply;
import static java.lang.System.out;

import com.geotab.api.Api;
import com.geotab.plain.objectmodel.maintenance.MaintenanceSeverity;
import com.geotab.plain.objectmodel.maintenance.MaintenanceWorkOrderJobSearch;
import com.geotab.plain.objectmodel.maintenance.MaintenanceWorkOrderSearch;
import com.geotab.plain.objectmodel.maintenance.MaintenanceWorkRequest;
import com.geotab.plain.objectmodel.maintenance.MaintenanceWorkRequestSearch;
import com.geotab.sdk.Util.Cmd;
import java.util.List;

public class MaintenanceApp {

  public static void main(String[] args) throws Exception {
    var cmd = new Cmd(MaintenanceApp.class);

    try (Api api = cmd.newApi()) {

      // Maintenance requests (potential work orders) with High or Critical severity
      var requests = api.callGet(MaintenanceWorkRequestEntity, apply(new MaintenanceWorkRequestSearch(), s -> {
        s.severities = List.of(MaintenanceSeverity.Critical, MaintenanceSeverity.High);
      }), 100).orElseThrow();

      for (MaintenanceWorkRequest request : requests) {
        out.format("🛠️MaintenanceWorkRequest [id=%s, severity=%s, dueOnDate=%s, type=%s, device=%s]%n",
            request.getId(), request.severity, request.dueOnDate,
            request.maintenanceType.getName() + "(" + request.maintenanceType.getId().getId() + ")",
            request.device.getName() + "(" + request.device.getId().getId() + ")");
      }
      out.println();

      // Pending maintenance orders
      var orders = api.callGet(MaintenanceWorkOrderEntity, apply(new MaintenanceWorkOrderSearch(), s -> {
        s.statuses = List.of(1); // 1 = Open/Pending
      }), 10).orElseThrow();
      for (var order : orders) {
        out.format("📝MaintenanceWorkOrder [id=%s, status=%s, reference=%s, device=%s]%n",
            order.getId(), order.statusCodeDisplay + "(" + order.statusCode + ")", order.reference,
            order.device.getName() + "(" + order.device.getId().getId() + ")");

        var jobs = api.callGet(MaintenanceWorkOrderJobEntity, apply(new MaintenanceWorkOrderJobSearch(), s -> {
          s.workOrderId = order.getId().getId();
        }), 100).orElseThrow();
        for (var job : jobs) {
          var typeInfo = job.maintenanceType.getName() + "(" + job.maintenanceType.source + ")";
          out.format("   🔧Job [id=%s, type=%s, closed=%s, date=%s]%n",
              job.getId(), typeInfo, job.isClosed, job.dateTime);
        }
        out.println();
      }
    }
  }
}
