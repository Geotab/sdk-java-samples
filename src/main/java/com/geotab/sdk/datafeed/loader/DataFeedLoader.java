package com.geotab.sdk.datafeed.loader;

import com.geotab.api.GeotabApi;
import com.geotab.http.exception.DbUnavailableException;
import com.geotab.http.exception.OverLimitException;
import com.geotab.http.invoker.ServerInvoker;
import com.geotab.model.login.Credentials;
import com.geotab.plain.Entities;
import com.geotab.plain.objectmodel.Driver;
import com.geotab.plain.objectmodel.User;
import com.geotab.plain.objectmodel.LogRecord;
import com.geotab.plain.objectmodel.Trip;
import com.geotab.plain.objectmodel.engine.FaultData;
import com.geotab.plain.objectmodel.engine.StatusData;
import com.geotab.sdk.datafeed.cache.ControllerCache;
import com.geotab.sdk.datafeed.cache.DeviceCache;
import com.geotab.sdk.datafeed.cache.DiagnosticCache;
import com.geotab.sdk.datafeed.cache.FailureModeCache;
import com.geotab.sdk.datafeed.cache.UnitOfMeasureCache;
import com.geotab.sdk.datafeed.cache.UserCache;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Data feed loader which queries Geotab's servers for vehicle data. */
public class DataFeedLoader {

  private static final Logger log = LoggerFactory.getLogger(DataFeedLoader.class);

  private final GeotabApi geotabApi;
  private final DataFeedParameters dataFeedParameters;
  private final ControllerCache controllerCache;
  private final UnitOfMeasureCache unitOfMeasureCache;
  private final DiagnosticCache diagnosticCache;
  private final FailureModeCache failureModeCache;
  private final DeviceCache deviceCache;
  private final UserCache userCache;
  private LocalDateTime cacheReloadTime;

  public DataFeedLoader(String serverUrl, Credentials credentials, DataFeedParameters feedParameters) {
    this.geotabApi = new GeotabApi(credentials, serverUrl, ServerInvoker.DEFAULT_TIMEOUT);
    this.dataFeedParameters = feedParameters;
    this.cacheReloadTime = LocalDateTime.now().minusMinutes(1);
    this.controllerCache = new ControllerCache(geotabApi);
    this.unitOfMeasureCache = new UnitOfMeasureCache(geotabApi);
    this.diagnosticCache = new DiagnosticCache(geotabApi, controllerCache, unitOfMeasureCache);
    this.failureModeCache = new FailureModeCache(geotabApi);
    this.deviceCache = new DeviceCache(geotabApi);
    this.userCache = new UserCache(geotabApi);
  }

  public DataFeedResult load() {
    log.debug("Loading data feed…");

    try {
      reloadCaches();

      // TODO loadExceptionEvents()

      var out = new DataFeedResult();
      out.gpsRecords = loadLogRecords();
      out.statusData = loadStatusData();
      out.faultData = loadFaultData();
      out.trips = loadTrips();
      return out;
    } catch (DbUnavailableException dbUnavailableException) {
      log.error("Db unavailable", dbUnavailableException);
      try {
        Thread.sleep(5 * 60 * 1000);
      } catch (InterruptedException e) {
        log.warn("Can not sleep due to DbUnavailableException", e);
      }
    } catch (OverLimitException overLimitException) {
      log.error("OverLimitException ({}); sleeping for 1 minute…  ", overLimitException.getMessage());
      try {
        Thread.sleep(60 * 1000);
      } catch (InterruptedException e) {
        log.warn("Can not sleep due to OverLimitException", e);
      }
    } catch (Exception exception) {
      log.error("Can not load data feed", exception);
    }

    var out = new DataFeedResult();
    out.gpsRecords = new ArrayList<>();
    out.statusData = new ArrayList<>();
    out.faultData = new ArrayList<>();
    out.trips = new ArrayList<>();
    return out;
  }

  public void stop() {
    geotabApi.disconnect();
  }

  private void reloadCaches() {
    if (LocalDateTime.now().isAfter(cacheReloadTime)) {
      log.debug("Reloading caches");

      controllerCache.reloadAll();
      unitOfMeasureCache.reloadAll();
      diagnosticCache.reloadAll();
      failureModeCache.reloadAll();
      deviceCache.reloadAll();
      userCache.reloadAll();
      // TODO ruleCache

      cacheReloadTime = LocalDateTime.now().plusHours(12);
    }
  }

  private List<LogRecord> loadLogRecords() {
    log.info("Getting LogRecord feed fromVersion {}", dataFeedParameters.lastGpsDataToken);
    var result = geotabApi.callGetFeed(Entities.LogRecordEntity, null, dataFeedParameters.lastGpsDataToken, null);
    if (result.isEmpty()) return Collections.emptyList();
    dataFeedParameters.lastGpsDataToken = result.get().getToVersion();

    return result.get().getData().stream().peek(logRecord -> {
      // Populate relevant LogRecord fields.
      if (logRecord.device != null && logRecord.device.getId() != null) {
        logRecord.device = deviceCache.get(logRecord.device.getId().getId());
      }
    }).toList();
  }

  private List<StatusData> loadStatusData() {
    log.info("Getting StatusData feed fromVersion {}", dataFeedParameters.lastStatusDataToken);
    var result = geotabApi.callGetFeed(Entities.StatusDataEntity, null, dataFeedParameters.lastStatusDataToken, null);
    if (result.isEmpty()) return Collections.emptyList();
    dataFeedParameters.lastStatusDataToken = result.get().getToVersion();

    return result.get().getData().stream().peek(data -> {
      // Populate relevant StatusData fields.
      if (data.device != null && data.device.getId() != null) {
        data.device = deviceCache.get(data.device.getId().getId());
      }
      if (data.diagnostic != null && data.diagnostic.getId() != null) {
        data.diagnostic = diagnosticCache.get(data.diagnostic.getId().getId());
      }
      if (data.controller != null && data.controller.getId() != null) {
        data.controller = controllerCache.get(data.controller.getId().getId());
      }
    }).toList();
  }

  private List<FaultData> loadFaultData() {
    log.info("Getting FaultData feed fromVersion {}", dataFeedParameters.lastFaultDataToken);
    var result = geotabApi.callGetFeed(Entities.FaultDataEntity, null, dataFeedParameters.lastFaultDataToken, null);
    if (result.isEmpty()) return Collections.emptyList();
    dataFeedParameters.lastFaultDataToken = result.get().getToVersion();

    return result.get().getData().stream().peek(data -> {
      // Populate relevant FaultData fields.
      if (data.device != null && data.device.getId() != null) {
        data.device = deviceCache.get(data.device.getId().getId());
      }
      if (data.diagnostic != null && data.diagnostic.getId() != null) {
        data.diagnostic = diagnosticCache.get(data.diagnostic.getId().getId());
      }
      if (data.controller != null && data.controller.getId() != null) {
        data.controller = controllerCache.get(data.controller.getId().getId());
      }
      if (data.failureMode != null && data.failureMode.getId() != null) {
        data.failureMode = failureModeCache.get(data.failureMode.getId().getId());
      }
    }).toList();
  }

  private List<Trip> loadTrips() {
    log.info("Getting Trips feed fromVersion {}", dataFeedParameters.lastTripToken);
    var result = geotabApi.callGetFeed(Entities.TripEntity, null, dataFeedParameters.lastTripToken, null);
    if (result.isEmpty()) return Collections.emptyList();
    dataFeedParameters.lastTripToken = result.get().getToVersion();

    return result.get().getData().stream().peek(trip -> {
      // Populate relevant Trip fields.
      if (trip.device != null && trip.device.getId() != null) {
        trip.device = deviceCache.get(trip.device.getId().getId());
      }
      if (trip.driver != null && trip.driver.getId() != null) {
        User cachedUser = userCache.get(trip.driver.getId().getId());
        if (cachedUser instanceof Driver d) {
          trip.driver = d;
        }
        // SystemUser (e.g. UnknownDriver) is not a Driver subtype — keep the slim SystemDriver
        // reference that was already deserialized from the wire.
      }
    }).toList();
  }
}
