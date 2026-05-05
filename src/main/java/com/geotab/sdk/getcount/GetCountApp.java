package com.geotab.sdk.getcount;

import static com.geotab.plain.Entities.AuditEntity;
import static com.geotab.plain.Entities.DVIRLogEntity;
import static com.geotab.plain.Entities.DeviceEntity;
import static com.geotab.plain.Entities.ExceptionEventEntity;
import static com.geotab.plain.Entities.RouteEntity;
import static com.geotab.plain.Entities.RuleEntity;
import static com.geotab.plain.Entities.UserEntity;
import static com.geotab.plain.Entities.ZoneEntity;
import static java.lang.System.out;

import com.geotab.api.Api;
import com.geotab.sdk.Util.Cmd;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetCountApp {

  private static final Logger log = LoggerFactory.getLogger(GetCountApp.class);
  private static final String TYPES = "Audit, Device, DVIRLog, Zone, ExceptionEvent, Route, Rule, User";

  private static final Map<String, Function<Api, Optional<Integer>>> COUNT_OF = Map.of(
    "Audit", api -> api.callGetCountOf(AuditEntity, null),
    "Device", api -> api.callGetCountOf(DeviceEntity, null),
    "DVIRLog", api -> api.callGetCountOf(DVIRLogEntity, null),
    "Zone", api -> api.callGetCountOf(ZoneEntity, null),
    "ExceptionEvent", api -> api.callGetCountOf(ExceptionEventEntity, null),
    "Route", api -> api.callGetCountOf(RouteEntity, null),
    "Rule", api -> api.callGetCountOf(RuleEntity, null),
    "User", api -> api.callGetCountOf(UserEntity, null)
  );

  public static void main(String[] args) throws Exception {
    Cmd cmd = new Cmd(GetCountApp.class);

    try (Api api = cmd.newApi()) {
      Scanner scan = new Scanner(System.in);
      while (true) {
        out.println();
        out.println("Please enter a entity type to get the count-of or 'exit' to exit (ex. " + TYPES + "): ");
        String type = scan.next();
        if (type.equalsIgnoreCase("exit")) System.exit(1);
        var counter = COUNT_OF.get(type);
        if (counter == null) {
          out.printf("Unknown entity type '%s'. Supported types: %s%n", type, TYPES);
          continue;
        }
        try {
          int resultCount = counter.apply(api).orElse(0);
          out.printf("Total Count of %s is: %s%n", type, resultCount);
        } catch (Exception ex) {
          log.error("Error executing GetCountOf<{}>", type, ex);
        }
      }
    }
  }
}
