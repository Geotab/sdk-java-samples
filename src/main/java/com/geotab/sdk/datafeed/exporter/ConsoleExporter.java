package com.geotab.sdk.datafeed.exporter;

import com.geotab.model.entity.NameEntity;
import com.geotab.plain.objectmodel.Device;
import com.geotab.plain.objectmodel.GoDevice;
import com.geotab.plain.objectmodel.LogRecord;
import com.geotab.plain.objectmodel.Trip;
import com.geotab.plain.objectmodel.XDevice;
import com.geotab.plain.objectmodel.engine.DataDiagnostic;
import com.geotab.plain.objectmodel.engine.FailureMode;
import com.geotab.plain.objectmodel.engine.FaultData;
import com.geotab.plain.objectmodel.engine.StatusData;
import com.geotab.sdk.datafeed.loader.DataFeedResult;
import com.google.common.collect.Iterables;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
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
      appendDeviceValues(stringBuilder, logRecord.device);
      appendValue(stringBuilder, Objects.toString(logRecord.dateTime, ""));
      appendValue(stringBuilder, round(logRecord.longitude));
      appendValue(stringBuilder, round(logRecord.latitude));
      appendValue(stringBuilder, logRecord.speed, false);

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
      appendDeviceValues(stringBuilder, data.device);
      appendValue(stringBuilder, Objects.toString(data.dateTime, ""));

      appendName(stringBuilder, data.diagnostic);
      appendName(stringBuilder, data.diagnostic.source);
      boolean isDataDiagnostic = data.diagnostic instanceof DataDiagnostic;
      appendValue(stringBuilder, data.data, isDataDiagnostic);
      if (isDataDiagnostic) {
        appendName(stringBuilder, data.diagnostic.unitOfMeasure, false);
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
      appendDeviceValues(stringBuilder, data.device);
      appendValue(stringBuilder, Objects.toString(data.dateTime, ""));

      appendName(stringBuilder, data.diagnostic);

      FailureMode failureMode = data.failureMode;
      appendName(stringBuilder, failureMode);
      if (failureMode == null || failureMode.isSystemEntity()) {
        appendValue(stringBuilder, "None");
      } else {
        appendName(stringBuilder, failureMode.source);
      }
      appendName(stringBuilder, data.controller, false);

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
      appendDeviceValues(stringBuilder, trip.device);
      appendValue(stringBuilder, trip.device instanceof GoDevice
          ? ((XDevice) trip.device).vehicleIdentificationNumber.replace(",", " ") : "");

      appendName(stringBuilder, trip.driver);
      appendValue(stringBuilder, Objects.toString(trip.start, ""));
      appendValue(stringBuilder, Objects.toString(trip.stop, ""));
      appendValue(stringBuilder, Objects.toString(trip.distance, ""));

      dataFeedBuilder.append(stringBuilder);
    }

    dataFeedBuilder.append("\n");
  }

  private void appendDeviceValues(StringBuilder stringBuilder, Device device) {
    if (device != null) {
      appendValue(stringBuilder, device.serialNumber);
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
    String name = entity.getName() != null ? entity.getName() : Objects.toString(entity.getId(), "");
    appendValue(stringBuilder, name.replace(",", " "), addSeparator);
  }

  private static double round(double value) {
    BigDecimal bigDecimal = new BigDecimal(Double.toString(value));
    bigDecimal = bigDecimal.setScale(3, RoundingMode.HALF_UP);
    return bigDecimal.doubleValue();
  }
}
