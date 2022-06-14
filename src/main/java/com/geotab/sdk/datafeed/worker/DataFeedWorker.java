package com.geotab.sdk.datafeed.worker;

import com.geotab.sdk.datafeed.cli.CommandLineArguments;
import com.geotab.sdk.datafeed.exporter.Exporter;
import com.geotab.sdk.datafeed.loader.DataFeedLoader;
import com.geotab.sdk.datafeed.loader.DataFeedResult;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataFeedWorker extends Thread {

  private static final Logger log = LoggerFactory.getLogger(DataFeedWorker.class);
  private AtomicBoolean isAlive = new AtomicBoolean(true);
  private AtomicBoolean isProccessing = new AtomicBoolean(false);

  private DataFeedLoader loader;
  private Exporter exporter;

  public DataFeedWorker(CommandLineArguments commandLineArguments) {
    this.loader = new DataFeedLoader(
        commandLineArguments.getServer(),
        commandLineArguments.getCredentials(),
        commandLineArguments.getDataFeedParameters()
    );
    this.exporter = Exporter.FACTORY.apply(commandLineArguments);
  }

  @Override
  public void run() {
    log.debug("Running…");

    try {
      isProccessing.set(true);

      while (isAlive.get()) {
        try {
          DataFeedResult dataFeedResult = loader.load();
          exporter.export(dataFeedResult);
        } catch (Exception exception) {
          log.error("Worker exception while processing", exception);
        }
      }

    } finally {
      loader.stop();
      isProccessing.set(false);
      log.debug("Processing stopped.");
    }
  }

  public void shutdown() {
    log.debug("Signal to stop processing…");
    isAlive.set(false);
  }

  public boolean isProcessing() {
    return isProccessing.get();
  }
}
