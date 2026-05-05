package com.geotab.sdk.datafeed.loader;

import com.geotab.plain.objectmodel.LogRecord;
import com.geotab.plain.objectmodel.Trip;
import com.geotab.plain.objectmodel.engine.FaultData;
import com.geotab.plain.objectmodel.engine.StatusData;

/** Contains latest data tokens. */
public class DataFeedParameters {

  /** The last ExceptionEvent token. */
  public String lastExceptionToken;

  /** The last {@link FaultData} token. */
  public String lastFaultDataToken;

  /** The last {@link LogRecord} token. */
  public String lastGpsDataToken;

  /** The last {@link StatusData} token. */
  public String lastStatusDataToken;

  /** The last {@link Trip} token. */
  public String lastTripToken;
}
