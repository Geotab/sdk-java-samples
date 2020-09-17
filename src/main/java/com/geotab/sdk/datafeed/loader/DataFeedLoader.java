package com.geotab.sdk.datafeed.loader;

import com.geotab.api.GeotabApi;
import com.geotab.http.exception.DbUnavailableException;
import com.geotab.http.exception.OverLimitException;
import com.geotab.http.invoker.ServerInvoker;
import com.geotab.http.request.AuthenticatedRequest;
import com.geotab.http.request.param.GetFeedParameters;
import com.geotab.http.response.GetFeedFaultDataResponse;
import com.geotab.http.response.GetFeedLogRecordResponse;
import com.geotab.http.response.GetFeedStatusDataResponse;
import com.geotab.http.response.GetFeedTripResponse;
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
import com.google.common.collect.ImmutableMap;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpException;

/**
 * Data feed loader which queries Geotab's servers for vehicle data.
 */
@Slf4j
public class DataFeedLoader {

  private static final Map<Class, Class> FEED_RESULT_TYPE = ImmutableMap.<Class, Class>builder()
      .put(LogRecord.class, GetFeedLogRecordResponse.class)
      .put(StatusData.class, GetFeedStatusDataResponse.class)
      .put(FaultData.class, GetFeedFaultDataResponse.class)
      .put(Trip.class, GetFeedTripResponse.class)
      .build();

  private GeotabApi geotabApi;

  private DataFeedParameters dataFeedParameters;

  private ControllerCache controllerCache;

  private UnitOfMeasureCache unitOfMeasureCache;

  private DiagnosticCache diagnosticCache;

  private FailureModeCache failureModeCache;

  private DeviceCache deviceCache;

  private DriverCache driverCache;

  private LocalDateTime cacheReloadTime;

  public DataFeedLoader(String serverUrl, Credentials credentials,
      DataFeedParameters feedParameters) {
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
    log.debug("Loading data feed ...");

    try {
      reloadCaches();

      List<LogRecord> logRecords = loadLogRecords();
      List<StatusData> statusData = loadStatusData();
      List<FaultData> faultData = loadFaultData();
      List<Trip> trips = loadTrips();
      // TODO loadExceptionEvents()

      return DataFeedResult.builder()
          .gpsRecords(logRecords)
          .statusData(statusData)
          .faultData(faultData)
          .trips(trips)
          .build();

    } catch (DbUnavailableException dbUnavailableException) {
      log.error("Db unavailable - ", dbUnavailableException);
      try {
        Thread.sleep(5 * 60 * 1000);
      } catch (InterruptedException e) {
        log.warn("Can not sleep due to DbUnavailableException", e);
      }
    } catch (OverLimitException overLimitException) {
      log.error("OverLimitException ({}); sleeping for 1 minute ...  ",
          overLimitException.getMessage());
      try {
        Thread.sleep(60 * 1000);
      } catch (InterruptedException e) {
        log.warn("Can not sleep due to OverLimitException", e);
      }
    } catch (HttpException httpException) {
      log.error("Http exception - ", httpException);
      try {
        Thread.sleep(5 * 1000);
      } catch (InterruptedException e) {
        log.warn("Can not sleep due to HttpException", e);
      }
    } catch (Exception exception) {
      log.error("Can not load data feed", exception);
    }

    return DataFeedResult.builder()
        .gpsRecords(new ArrayList<>())
        .statusData(new ArrayList<>())
        .faultData(new ArrayList<>())
        .trips(new ArrayList<>())
        .build();
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

  private List<LogRecord> loadLogRecords() throws Exception {
    Optional<FeedResult<LogRecord>> logRecordFeedResult = getFeed(LogRecord.class,
        dataFeedParameters.getLastGpsDataToken());

    List<LogRecord> logRecords = new ArrayList<>();
    if (logRecordFeedResult.isPresent()) {
      dataFeedParameters.setLastGpsDataToken(logRecordFeedResult.get().getToVersion());
      logRecordFeedResult.get().getData()
          .forEach(logRecord -> {
            // Populate relevant LogRecord fields.
            logRecord.setDevice(deviceCache.get(logRecord.getDevice().getId().getId()));
            logRecords.add(logRecord);
          });
    }

    return logRecords;
  }

  private List<StatusData> loadStatusData() throws Exception {
    Optional<FeedResult<StatusData>> statusDataFeedResult = getFeed(StatusData.class,
        dataFeedParameters.getLastStatusDataToken());

    List<StatusData> statusData = new ArrayList<>();
    if (statusDataFeedResult.isPresent()) {
      dataFeedParameters.setLastStatusDataToken(statusDataFeedResult.get().getToVersion());
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

  private List<FaultData> loadFaultData() throws Exception {
    Optional<FeedResult<FaultData>> faultDataFeedResult = getFeed(FaultData.class,
        dataFeedParameters.getLastFaultDataToken());

    List<FaultData> faultData = new ArrayList<>();
    if (faultDataFeedResult.isPresent()) {
      dataFeedParameters.setLastFaultDataToken(faultDataFeedResult.get().getToVersion());
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

  private List<Trip> loadTrips() throws Exception {
    Optional<FeedResult<Trip>> tripFeedResult = getFeed(Trip.class,
        dataFeedParameters.getLastTripToken());

    List<Trip> trips = new ArrayList<>();
    if (tripFeedResult.isPresent()) {
      dataFeedParameters.setLastTripToken(tripFeedResult.get().getToVersion());
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

  private <T extends Entity> Optional<FeedResult<T>> getFeed(Class<T> type, String fromVersion)
      throws Exception {

    log.info("Get data feed for {} fromVersion {}", type.getSimpleName(), fromVersion);

    Class responseType = FEED_RESULT_TYPE.get(type);
    if (responseType == null) {
      log.warn("GetFeed for {} is not supported", type);
      return Optional.empty();
    }

    AuthenticatedRequest<?> request = AuthenticatedRequest.authRequestBuilder()
        .method("GetFeed")
        .params(GetFeedParameters.getFeedParamsBuilder()
            .typeName(type.getSimpleName())
            .fromVersion(fromVersion)
            .build())
        .build();

    return geotabApi.call(request, responseType);
  }

}
