package com.geotab.sdk.datafeed.loader;

import com.geotab.plain.objectmodel.LogRecord;
import com.geotab.plain.objectmodel.Trip;
import com.geotab.plain.objectmodel.engine.FaultData;
import com.geotab.plain.objectmodel.engine.StatusData;
import java.util.List;

/** The result of a Feed call. */
public class DataFeedResult {

  public List<LogRecord> gpsRecords;

  public List<StatusData> statusData;

  public List<FaultData> faultData;

  public List<Trip> trips;

  // TODO private List<ExceptionEvent> exceptionEvents;
}
