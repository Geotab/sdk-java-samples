package com.geotab.sdk.datafeed;

import static com.geotab.sdk.Util.Arg;
import static com.geotab.sdk.Util.Cmd;

import com.geotab.sdk.datafeed.exporter.Exporter;
import com.geotab.sdk.datafeed.loader.DataFeedParameters;
import com.geotab.sdk.datafeed.worker.DataFeedWorker;
import com.google.common.base.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataFeedApp {

  private static final Logger log = LoggerFactory.getLogger(DataFeedApp.class);

  public static void main(String[] args) throws Exception {
    Cmd cmd = new Cmd(DataFeedApp.class,
      new Arg("gpsToken",         false, "Last known GPS data token"),
      new Arg("statusToken",      false, "Last known status data token"),
      new Arg("faultToken",       false, "Last known fault data token"),
      new Arg("tripToken",        false, "Last known trip token"),
      new Arg("exceptionToken",   false, "Last known exception token"),
      new Arg("exportType",       false, "Export type: console (default) or csv"),
      new Arg("outputFolder",     false, "Output folder for CSV files (default: current directory)"),
      new Arg("feedContinuously", false, "Run continuously: true or false (default: false)")
    );

    DataFeedParameters params = new DataFeedParameters();
    params.lastGpsDataToken    = Optional.ofNullable(cmd.get("gpsToken")).orElse("0");
    params.lastStatusDataToken = Optional.ofNullable(cmd.get("statusToken")).orElse("0");
    params.lastFaultDataToken  = Optional.ofNullable(cmd.get("faultToken")).orElse("0");
    params.lastTripToken       = Optional.ofNullable(cmd.get("tripToken")).orElse("0");
    params.lastExceptionToken  = Optional.ofNullable(cmd.get("exceptionToken")).orElse("0");

    boolean feedContinuously = "true".equalsIgnoreCase(cmd.get("feedContinuously"));
    Exporter exporter = Exporter.create(cmd.get("exportType"), cmd.get("outputFolder"));

    DataFeedWorker worker = new DataFeedWorker(cmd.server, cmd.credentials, params, exporter);
    addShutdownHook(worker);
    worker.start();

    if (!feedContinuously) {
      while (true) {
        if (worker.isProcessing()) {
          worker.shutdown();
          break;
        }
      }
    }

    worker.join();
  }

  private static void addShutdownHook(DataFeedWorker worker) {
    final Thread mainThread = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.debug("Application is stopping…");
      if (worker.isProcessing()) {
        worker.shutdown();
      }
      try {
        mainThread.join();
      } catch (InterruptedException e) {
        log.error("Can not join main thread");
      }
      log.debug("Application stopped");
    }));
  }
}
