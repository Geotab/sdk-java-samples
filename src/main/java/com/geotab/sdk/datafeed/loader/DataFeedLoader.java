package com.geotab.sdk.datafeed.loader;

import com.geotab.api.GeotabApi;
import com.geotab.http.exception.DbUnavailableException;
import com.geotab.http.exception.OverLimitException;
import com.geotab.http.invoker.ServerInvoker;
import com.geotab.http.request.param.GetFeedParameters;
import com.geotab.model.FeedResult;
import com.geotab.model.entity.Entity;
import com.geotab.model.entity.faultdata.FaultData;
import com.geotab.model.entity.logrecord.LogRecord;
import com.geotab.model.entity.statusdata.StatusData;
import com.geotab.model.entity.trip.Trip;
import com.geotab.model.login.Credentials;
import com.geotab.sdk.datafeed.cache.ControllerCache;
import com.geotab.sdk.datafeed.cache.DeviceCache;
import com.geotab.sdk.datafeed.cache.DiagnosticCache;
import com.geotab.sdk.datafeed.cache.DriverCache;
import com.geotab.sdk.datafeed.cache.FailureModeCache;
import com.geotab.sdk.datafeed.cache.UnitOfMeasureCache;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data feed loader which queries Geotab's servers for vehicle data.
 */
public class DataFeedLoader {

  private static final Logger log = LoggerFactory.getLogger(DataFeedLoader.class);

  private GeotabApi geotabApi;

  private DataFeedParameters dataFeedParameters;

  private ControllerCache controllerCache;

  private UnitOfMeasureCache unitOfMeasureCache;

  private DiagnosticCache diagnosticCache;

  private FailureModeCache failureModeCache;

  private DeviceCache deviceCache;

  private DriverCache driverCache;

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
    this.driverCache = new DriverCache(geotabApi);
  }

  public DataFeedResult load() {
    log.debug("Loading data feed…");

    try {
      reloadCaches();

      // TODO loadExceptionEvents()

      DataFeedResult out = new DataFeedResult();
      out.gpsRecords = loadLogRecords();
      out.statusData = loadStatusData();
      out.faultData = loadFaultData();
      out.trips = loadTrips();
      return out;
    } catch (DbUnavailableException dbUnavailableException) {
      log.error("Db unavailable - ", dbUnavailableException);
      try {
        Thread.sleep(5 * 60 * 1000);
      } catch (InterruptedException e) {
        log.warn("Can not sleep due to DbUnavailableException", e);
      }
    } catch (OverLimitException overLimitException) {
      log.error("OverLimitException ({}); sleeping for 1 minute…  ",
          overLimitException.getMessage());
      try {
        Thread.sleep(60 * 1000);
      } catch (InterruptedException e) {
        log.warn("Can not sleep due to OverLimitException", e);
      }
    } catch (Exception exception) {
      log.error("Can not load data feed", exception);
    }

    DataFeedResult out = new DataFeedResult();
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
      driverCache.reloadAll();
      //TODO ruleCache

      cacheReloadTime = LocalDateTime.now().plusHours(12);
    }
  }

  private List<LogRecord> loadLogRecords() {
    Optional<FeedResult<LogRecord>> logRecordFeedResult = getFeed(LogRecord.class,
        dataFeedParameters.lastGpsDataToken);

    List<LogRecord> logRecords = new ArrayList<>();
    if (logRecordFeedResult.isPresent()) {
      dataFeedParameters.lastGpsDataToken = logRecordFeedResult.get().getToVersion();
      logRecordFeedResult.get().getData()
          .forEach(logRecord -> {
            // Populate relevant LogRecord fields.
            logRecord.setDevice(deviceCache.get(logRecord.getDevice().getId().getId()));
            logRecords.add(logRecord);
          });
    }

    return logRecords;
  }

  private List<StatusData> loadStatusData() {
    Optional<FeedResult<StatusData>> statusDataFeedResult = getFeed(StatusData.class,
        dataFeedParameters.lastStatusDataToken);

    List<StatusData> statusData = new ArrayList<>();
    if (statusDataFeedResult.isPresent()) {
      dataFeedParameters.lastStatusDataToken = statusDataFeedResult.get().getToVersion();
      statusDataFeedResult.get().getData()
          .forEach(data -> {
            // Populate relevant StatusData fields.
            data.setDevice(deviceCache.get(data.getDevice().getId().getId()));
            data.setDiagnostic(diagnosticCache.get(data.getDiagnostic().getId().getId()));
            data.setController(controllerCache.get(data.getController().getId().getId()));
            statusData.add(data);
          });
    }

    return statusData;
  }

  private List<FaultData> loadFaultData() {
    Optional<FeedResult<FaultData>> faultDataFeedResult = getFeed(FaultData.class,
        dataFeedParameters.lastFaultDataToken);

    List<FaultData> faultData = new ArrayList<>();
    if (faultDataFeedResult.isPresent()) {
      dataFeedParameters.lastFaultDataToken = faultDataFeedResult.get().getToVersion();
      faultDataFeedResult.get().getData()
          .forEach(data -> {
            // Populate relevant FaultData fields.
            data.setDevice(deviceCache.get(data.getDevice().getId().getId()));
            data.setDiagnostic(diagnosticCache.get(data.getDiagnostic().getId().getId()));
            data.setController(controllerCache.get(data.getController().getId().getId()));
            data.setFailureMode(failureModeCache.get(data.getFailureMode().getId().getId()));
            faultData.add(data);
          });
    }

    return faultData;
  }

  private List<Trip> loadTrips() {
    Optional<FeedResult<Trip>> tripFeedResult = getFeed(Trip.class, dataFeedParameters.lastTripToken);

    List<Trip> trips = new ArrayList<>();
    if (tripFeedResult.isPresent()) {
      dataFeedParameters.lastTripToken = tripFeedResult.get().getToVersion();
      tripFeedResult.get().getData()
          .forEach(trip -> {
            // Populate relevant Trip fields.
            trip.setDevice(deviceCache.get(trip.getDevice().getId().getId()));
            trip.setDriver(driverCache.get(trip.getDriver().getId().getId()));
            trips.add(trip);
          });
    }

    return trips;
  }

  private <T extends Entity> Optional<FeedResult<T>> getFeed(Class<T> type, String fromVersion) {
    log.info("Get data feed for {} fromVersion {}", type.getSimpleName(), fromVersion);
    return geotabApi.callGetFeed(GetFeedParameters.getFeedParamsBuilder()
        .typeName(type.getSimpleName()).fromVersion(fromVersion).build(), type);
  }
}
