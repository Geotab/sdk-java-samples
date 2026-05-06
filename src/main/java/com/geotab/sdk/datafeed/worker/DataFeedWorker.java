package com.geotab.sdk.datafeed.worker;

import com.geotab.model.login.Credentials;
import com.geotab.sdk.datafeed.exporter.Exporter;
import com.geotab.sdk.datafeed.loader.DataFeedLoader;
import com.geotab.sdk.datafeed.loader.DataFeedParameters;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataFeedWorker extends Thread {

  private static final Logger log = LoggerFactory.getLogger(DataFeedWorker.class);
  private final AtomicBoolean isAlive = new AtomicBoolean(true);
  private final AtomicBoolean isProcessing = new AtomicBoolean(false);
  private final DataFeedLoader loader;
  private final Exporter exporter;

  public DataFeedWorker(
    String server, Credentials credentials, DataFeedParameters params, Exporter exporter) {
    this.loader = new DataFeedLoader(server, credentials, params);
    this.exporter = exporter;
  }

  @Override
  public void run() {
    log.debug("Running…");

    try {
      isProcessing.set(true);

      while (isAlive.get()) {
        try {
          exporter.export(loader.load());
        } catch (Exception exception) {
          log.error("Worker exception while processing", exception);
        }
      }

    } finally {
      loader.stop();
      isProcessing.set(false);
      log.debug("Processing stopped.");
    }
  }

  public void shutdown() {
    log.debug("Signal to stop processing…");
    isAlive.set(false);
  }

  public boolean isProcessing() {
    return isProcessing.get();
  }
}
