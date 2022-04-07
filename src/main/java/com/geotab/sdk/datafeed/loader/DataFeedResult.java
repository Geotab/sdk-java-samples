package com.geotab.sdk.datafeed.loader;

import com.geotab.model.entity.faultdata.FaultData;
import com.geotab.model.entity.logrecord.LogRecord;
import com.geotab.model.entity.statusdata.StatusData;
import com.geotab.model.entity.trip.Trip;
import java.util.List;

/**
 * The result of a Feed call.
 */
public class DataFeedResult {

  public List<LogRecord> gpsRecords;

  public List<StatusData> statusData;

  public List<FaultData> faultData;

  public List<Trip> trips;

  // TODO private List<ExceptionEvent> exceptionEvents;
}
