package com.geotab.sdk.datafeed.loader;

import com.geotab.model.entity.faultdata.FaultData;
import com.geotab.model.entity.logrecord.LogRecord;
import com.geotab.model.entity.statusdata.StatusData;
import com.geotab.model.entity.trip.Trip;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The result of a Feed call.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataFeedResult {

  private List<LogRecord> gpsRecords;

  private List<StatusData> statusData;

  private List<FaultData> faultData;

  private List<Trip> trips;

  // TODO private List<ExceptionEvent> exceptionEvents;
}
