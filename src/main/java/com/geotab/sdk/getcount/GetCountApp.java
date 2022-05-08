package com.geotab.sdk.getcount;

import static com.geotab.http.invoker.ServerInvoker.DEFAULT_TIMEOUT;
import static java.lang.System.out;

import com.geotab.api.Api;
import com.geotab.api.GeotabApi;
import com.geotab.http.request.AuthenticatedRequest;
import com.geotab.http.request.param.SearchParameters;
import com.geotab.http.response.CountResponse;
import com.geotab.sdk.Util.Cmd;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetCountApp {

  private static final Logger log = LoggerFactory.getLogger(GetCountApp.class);
  private static final String TYPES = "Audit, Device, DVIRLog, Zone, ExceptionEvent, Route, Rule, Trailer, User";

  public static void main(String[] args) {
    Cmd cmd = new Cmd(GetCountApp.class);

    try (Api api = new GeotabApi(cmd.credentials, cmd.server, DEFAULT_TIMEOUT)) {
      Scanner scan = new Scanner(System.in);
      while (true) {
        out.println();
        out.println("Please enter a entity type to get the count-of or leave it empty to exit (ex. " + TYPES + "): ");
        String type = scan.next();
        if (type.isBlank()) System.exit(1);
        try {
          AuthenticatedRequest<?> countOfRequest = AuthenticatedRequest.authRequestBuilder().method("GetCountOf")
              .params(SearchParameters.searchParamsBuilder().typeName(type).build()).build();
          Integer resultCount = api.call(countOfRequest, CountResponse.class).orElse(0);
          out.printf("Total Count of %s is: %s%n", type, resultCount);
        } catch (Exception ex) {
          log.error("Error executing GetCountOf<{}>", type, ex);
        }
      }
    }
  }
}
