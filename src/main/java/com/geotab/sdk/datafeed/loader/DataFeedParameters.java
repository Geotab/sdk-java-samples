package com.geotab.sdk.datafeed.loader;

import com.geotab.model.entity.faultdata.FaultData;
import com.geotab.model.entity.logrecord.LogRecord;
import com.geotab.model.entity.statusdata.StatusData;
import com.geotab.model.entity.trip.Trip;

/**
 * Contains latest data tokens.
 */
public class DataFeedParameters {

  /**
   * The last ExceptionEvent token.
   */
  public String lastExceptionToken;

  /**
   * The last {@link FaultData} token.
   */
  public String lastFaultDataToken;

  /**
   * The last {@link LogRecord} token.
   */
  public String lastGpsDataToken;

  /**
   * The last {@link StatusData} token.
   */
  public String lastStatusDataToken;

  /**
   * The last {@link Trip} token.
   */
  public String lastTripToken;
}
