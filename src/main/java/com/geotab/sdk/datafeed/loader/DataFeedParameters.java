package com.geotab.sdk.datafeed.loader;

import com.geotab.model.entity.faultdata.FaultData;
import com.geotab.model.entity.logrecord.LogRecord;
import com.geotab.model.entity.statusdata.StatusData;
import com.geotab.model.entity.trip.Trip;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contains latest data tokens.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataFeedParameters {

  /**
   * The last ExceptionEvent token.
   */
  private String lastExceptionToken;

  /**
   * The last {@link FaultData} token.
   */
  private String lastFaultDataToken;

  /**
   * The last {@link LogRecord} token.
   */
  private String lastGpsDataToken;

  /**
   * The last {@link StatusData} token.
   */
  private String lastStatusDataToken;

  /**
   * The last {@link Trip} token.
   */
  private String lastTripToken;
}
