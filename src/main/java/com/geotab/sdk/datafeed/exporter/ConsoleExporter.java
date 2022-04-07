package com.geotab.sdk.datafeed.exporter;

import static com.geotab.util.DateTimeUtil.localDateTimeToString;

import com.geotab.model.entity.NameEntity;
import com.geotab.model.entity.device.Device;
import com.geotab.model.entity.device.GoDevice;
import com.geotab.model.entity.diagnostic.DataDiagnostic;
import com.geotab.model.entity.failuremode.FailureMode;
import com.geotab.model.entity.failuremode.NoFailureMode;
import com.geotab.model.entity.faultdata.FaultData;
import com.geotab.model.entity.logrecord.LogRecord;
import com.geotab.model.entity.statusdata.StatusData;
import com.geotab.model.entity.trip.Trip;
import com.geotab.sdk.datafeed.loader.DataFeedResult;
import com.google.common.collect.Iterables;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleExporter implements Exporter {

  //@formatter:off
  private static final String GPS_DATA_HEADER = "Vehicle Serial Number, Date, Longitude, Latitude, Speed";
  private static final String STATUS_DATA_HEADER = "Vehicle Serial Number, Date, Diagnostic Name, Source Name, Value, Units";
  private static final String FAULT_DATA_HEADER = "Vehicle Serial Number, Date, Diagnostic Name, Failure Mode Name, Failure Mode Source, Controller Name";
  private static final String TRIP_HEADER = "Vehicle Serial Number, Vin, Driver Name, Trip Start Time, Trip End Time,Trip Distance";
  //@formatter:on
  private static final Logger log = LoggerFactory.getLogger(ConsoleExporter.class);

  public void export(DataFeedResult dataFeedResult) {
    StringBuilder dataFeedBuilder = new StringBuilder("\n\n\n");
    appendLogRecords(dataFeedBuilder, dataFeedResult.gpsRecords);
    appendStatusData(dataFeedBuilder, dataFeedResult.statusData);
    appendFaultData(dataFeedBuilder, dataFeedResult.faultData);
    appendTrips(dataFeedBuilder, dataFeedResult.trips);
    dataFeedBuilder.append("\n\n\n");
    log.info(dataFeedBuilder.toString());
  }

  private void appendLogRecords(StringBuilder dataFeedBuilder, List<LogRecord> logRecords) {
    if (Iterables.isEmpty(logRecords)) return;

    dataFeedBuilder.append("\n");
    dataFeedBuilder.append(GPS_DATA_HEADER);

    for (LogRecord logRecord : logRecords) {
      StringBuilder stringBuilder = new StringBuilder("\n");
      appendDeviceValues(stringBuilder, logRecord.getDevice());
      appendValue(stringBuilder, localDateTimeToString(logRecord.getDateTime()));
      appendValue(stringBuilder, round(logRecord.getLongitude(), 3));
      appendValue(stringBuilder, round(logRecord.getLatitude(), 3));
      appendValue(stringBuilder, logRecord.getSpeed(), false);

      dataFeedBuilder.append(stringBuilder);
    }

    dataFeedBuilder.append("\n");
  }

  private void appendStatusData(StringBuilder dataFeedBuilder, List<StatusData> statusData) {
    if (Iterables.isEmpty(statusData)) {
      return;
    }

    dataFeedBuilder.append("\n");
    dataFeedBuilder.append(STATUS_DATA_HEADER);

    for (StatusData data : statusData) {
      StringBuilder stringBuilder = new StringBuilder("\n");
      appendDeviceValues(stringBuilder, data.getDevice());
      appendValue(stringBuilder, localDateTimeToString(data.getDateTime()));

      appendName(stringBuilder, data.getDiagnostic());
      appendName(stringBuilder, data.getDiagnostic().getSource());
      boolean isDataDiagnostic = data.getDiagnostic() instanceof DataDiagnostic;
      appendValue(stringBuilder, data.getData(), isDataDiagnostic);
      if (isDataDiagnostic) {
        appendName(stringBuilder, data.getDiagnostic().getUnitOfMeasure(), false);
      }

      dataFeedBuilder.append(stringBuilder);
    }

    dataFeedBuilder.append("\n");
  }

  private void appendFaultData(StringBuilder dataFeedBuilder, List<FaultData> faultData) {
    if (Iterables.isEmpty(faultData)) {
      return;
    }

    dataFeedBuilder.append("\n");
    dataFeedBuilder.append(FAULT_DATA_HEADER);

    for (FaultData data : faultData) {
      StringBuilder stringBuilder = new StringBuilder("\n");
      appendDeviceValues(stringBuilder, data.getDevice());
      appendValue(stringBuilder, localDateTimeToString(data.getDateTime()));

      appendName(stringBuilder, data.getDiagnostic());

      FailureMode failureMode = data.getFailureMode();
      appendName(stringBuilder, failureMode);
      if (NoFailureMode.getInstance().equals(failureMode)) {
        appendValue(stringBuilder, "None");
      } else {
        appendName(stringBuilder, failureMode.getSource());
      }
      appendName(stringBuilder, data.getController(), false);

      dataFeedBuilder.append(stringBuilder);
    }

    dataFeedBuilder.append("\n");
  }

  private void appendTrips(StringBuilder dataFeedBuilder, List<Trip> trips) {
    if (Iterables.isEmpty(trips)) {
      return;
    }

    dataFeedBuilder.append("\n");
    dataFeedBuilder.append(TRIP_HEADER);

    for (Trip trip : trips) {
      StringBuilder stringBuilder = new StringBuilder("\n");
      appendDeviceValues(stringBuilder, trip.getDevice());
      appendValue(stringBuilder, trip.getDevice() instanceof GoDevice
          ? ((GoDevice) trip.getDevice()).getVehicleIdentificationNumber().replace(",", " ") : "");

      appendName(stringBuilder, trip.getDriver());
      appendValue(stringBuilder, trip.getStart() != null ? localDateTimeToString(trip.getStart()) : "");
      appendValue(stringBuilder, trip.getStop() != null ? localDateTimeToString(trip.getStop()) : "");
      appendValue(stringBuilder, trip.getDistance() != null ? trip.getDistance() : "");

      dataFeedBuilder.append(stringBuilder);
    }

    dataFeedBuilder.append("\n");
  }

  private void appendDeviceValues(StringBuilder stringBuilder, Device device) {
    if (device != null) {
      appendValue(stringBuilder, device.getSerialNumber());
    } else {
      appendValue(stringBuilder, "");
      appendValue(stringBuilder, "");
    }
  }

  private void appendValue(StringBuilder stringBuilder, Object object) {
    appendValue(stringBuilder, object, true);
  }


  private void appendValue(StringBuilder stringBuilder, Object object, boolean addSeparator) {
    stringBuilder.append(object);
    if (addSeparator) {
      stringBuilder.append(", ");
    }
  }

  private void appendName(StringBuilder stringBuilder, NameEntity entity) {
    appendName(stringBuilder, entity, true);
  }

  private void appendName(StringBuilder stringBuilder, NameEntity entity, boolean addSeparator) {
    appendValue(stringBuilder,
        entity.isSystemEntity() ? entity.getClass().getSimpleName().replace(",", " ")
            : entity.getName().replace(",", " "), addSeparator);
  }

  private static double round(double value, int places) {
    if (places < 0) {
      throw new IllegalArgumentException();
    }

    BigDecimal bigDecimal = new BigDecimal(Double.toString(value));
    bigDecimal = bigDecimal.setScale(places, RoundingMode.HALF_UP);
    return bigDecimal.doubleValue();
  }
}
