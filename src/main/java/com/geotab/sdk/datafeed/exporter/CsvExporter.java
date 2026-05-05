package com.geotab.sdk.datafeed.exporter;

import static com.geotab.util.DateTimeUtil.nowUtcLocalDateTime;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.geotab.model.entity.NameEntity;
import com.geotab.plain.objectmodel.GoDevice;
import com.geotab.plain.objectmodel.LogRecord;
import com.geotab.plain.objectmodel.Trip;
import com.geotab.plain.objectmodel.XDevice;
import com.geotab.plain.objectmodel.engine.DataDiagnostic;
import com.geotab.plain.objectmodel.engine.FaultData;
import com.geotab.plain.objectmodel.engine.StatusData;
import com.geotab.sdk.datafeed.loader.DataFeedResult;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvExporter implements Exporter {

  private static final String[] GPS_DATA_HEADER = new String[] {
    "Vehicle Name", "Vehicle Serial Number", "VIN", "Date", "Longitude", "Latitude", "Speed" };

  private static final String GPS_FILE_NAME_PREFIX = "Gps_Data";

  private static final String[] STATUS_DATA_HEADER = new String[] {
    "Vehicle Name", "Vehicle Serial Number", "VIN", "Date", "Diagnostic Name", "Diagnostic Code",
    "Source Name", "Value", "Units" };

  private static final String STATUS_DATA_FILE_NAME_PREFIX = "Status_Data";

  private static final String[] FAULT_DATA_HEADER = new String[] {
    "Vehicle Name", "Vehicle Serial Number", "VIN", "Date", "Diagnostic Name", "Failure Mode Name",
    "Failure Mode Code", "Failure Mode Source", "Controller Name", "Count", "Active",
    "Malfunction Lamp", "Red Stop Lamp", "Amber Warning Lamp", "Protect Lamp", "Dismiss Date",
    "Dismiss User" };

  private static final String FAULT_DATA_FILE_NAME_PREFIX = "Fault_Data";

  private static final String[] TRIP_HEADER = new String[] {
    "VehicleName", "VehicleSerialNumber", "Vin", "Driver Name", "Driver Keys", "Trip Start Time",
    "Trip End Time", "Trip Distance" };

  private static final String TRIP_FILE_NAME_PREFIX = "Trips";
  private static final Logger log = LoggerFactory.getLogger(CsvExporter.class);

  private String outputPath;

  public CsvExporter(String outputPath) {
    this.outputPath = outputPath != null && !outputPath.isEmpty() ? outputPath : ".";

    if (!Files.exists(Paths.get(this.outputPath))) {
      try {
        Files.createDirectories(Paths.get(outputPath));
      } catch (IOException e) {
        throw new RuntimeException("Failed to initialize for output path " + outputPath, e);
      }
    }
  }

  public void export(DataFeedResult dataFeedResult) throws Exception {
    exportLogRecords(dataFeedResult.gpsRecords);
    exportStatusData(dataFeedResult.statusData);
    exportFaultData(dataFeedResult.faultData);
    exportTrips(dataFeedResult.trips);
  }

  private void exportLogRecords(List<LogRecord> logRecords) throws Exception {
    log.debug("Exporting LogRecords to csv…");

    String csvFile = generateCsv(GPS_FILE_NAME_PREFIX, GPS_DATA_HEADER, transformLogRecords(logRecords));

    log.info("LogRecords exported to {}", csvFile);
  }

  private void exportStatusData(List<StatusData> statusData) throws Exception {
    log.debug("Exporting StatusData to csv…");

    String csvFile = generateCsv(STATUS_DATA_FILE_NAME_PREFIX, STATUS_DATA_HEADER, transformStatusData(statusData));

    log.info("StatusData exported to {}", csvFile);
  }

  private void exportFaultData(List<FaultData> faultData) throws Exception {
    log.debug("Exporting FaultData to csv…");

    String csvFile = generateCsv(FAULT_DATA_FILE_NAME_PREFIX, FAULT_DATA_HEADER, transformFaultData(faultData));

    log.info("FaultData exported to {}", csvFile);
  }

  private void exportTrips(List<Trip> trips) throws Exception {
    log.debug("Exporting Trips to csv…");

    String csvFile = generateCsv(TRIP_FILE_NAME_PREFIX, TRIP_HEADER, transformTrips(trips));

    log.info("Trips exported to {}", csvFile);
  }

  private String generateCsv(String fileNamePrefix, String[] headers, List<String[]> csvRows)
    throws Exception {

    String reportFileName = "%s-%s.csv".formatted(fileNamePrefix,
      nowUtcLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")));
    Path reportFilePath = Paths.get(outputPath + File.separator + reportFileName);

    boolean addHeaderRow = !Files.exists(reportFilePath);

    try (Writer writer = new FileWriter(reportFilePath.toString(), true)) {
      if (addHeaderRow) {
        writer.append(String.join(",", headers));
      }

      for (String[] row : csvRows) {
        writer.append(System.lineSeparator()).append(String.join(",", row));
      }
    }

    return reportFilePath.toString();
  }

  private List<String[]> transformLogRecords(List<LogRecord> logRecords) {
    if (Iterables.isEmpty(logRecords)) {
      return new ArrayList<>();
    }

    return logRecords.stream()
      .map(logRecord -> stream(new String[] {
        logRecord.device.getName().replace(",", " "),
        logRecord.device.serialNumber,
        logRecord.device instanceof GoDevice
          ? ((XDevice) logRecord.device)
            .vehicleIdentificationNumber.replace(",", " ")
          : "",
        Objects.toString(logRecord.dateTime, ""),
        Objects.toString(logRecord.longitude, ""),
        Objects.toString(logRecord.latitude, ""),
        Objects.toString(logRecord.speed, ""),
      }).map(CsvExporter::escapeCsv).toArray(String[]::new))
      .collect(Collectors.toList());
  }

  private List<String[]> transformStatusData(List<StatusData> statusData) {
    if (Iterables.isEmpty(statusData)) {
      return new ArrayList<>();
    }

    return statusData.stream()
      .map(data -> stream(new String[] {
        data.device.getName().replace(",", " "),
        data.device.serialNumber,
        data.device instanceof GoDevice
          ? ((XDevice) data.device)
            .vehicleIdentificationNumber.replace(",", " ")
          : "",
        Objects.toString(data.dateTime, ""),
        getName(data.diagnostic),
        Objects.toString(data.diagnostic.code, ""),
        Optional.ofNullable(data.diagnostic.source).map(this::getName).orElse(""),
        Objects.toString(data.data, ""),
        data.diagnostic instanceof DataDiagnostic
          ? getName(data.diagnostic.unitOfMeasure)
          : ""
      }).map(CsvExporter::escapeCsv).toArray(String[]::new))
      .collect(Collectors.toList());
  }

  private List<String[]> transformFaultData(List<FaultData> faultData) {
    if (Iterables.isEmpty(faultData)) {
      return new ArrayList<>();
    }

    return faultData.stream()
      .map(data -> stream(new String[] {
        data.device.getName().replace(",", " "),
        data.device.serialNumber,
        data.device instanceof GoDevice
          ? ((XDevice) data.device)
            .vehicleIdentificationNumber.replace(",", " ")
          : "",
        Objects.toString(data.dateTime, ""),
        getName(data.diagnostic),
        getName(data.failureMode),
        Optional.ofNullable(data.failureMode)
          .map(fm -> fm.code)
          .map(Object::toString)
          .orElse(""),
        data.failureMode == null || data.failureMode.isSystemEntity()
          ? "None"
          : getName(data.failureMode.source),
        getName(data.controller),
        Objects.toString(data.count, ""),
        Objects.toString(data.faultState, ""),
        Objects.toString(data.malfunctionLamp, ""),
        Objects.toString(data.redStopLamp, ""),
        Objects.toString(data.amberWarningLamp, ""),
        Objects.toString(data.protectWarningLamp, ""),
        Objects.toString(data.dismissDateTime, ""),
        Optional.ofNullable(data.dismissUser)
          .map(u -> u.getName().replace(",", " "))
          .orElse("")
      }).map(CsvExporter::escapeCsv).toArray(String[]::new))
      .collect(Collectors.toList());
  }

  private List<String[]> transformTrips(List<Trip> trips) {
    if (Iterables.isEmpty(trips)) return new ArrayList<>();

    return trips.stream()
      .map(trip -> new String[] {
        trip.device.getName().replace(",", " "),
        trip.device.serialNumber,
        !(trip.device instanceof GoDevice)
          ? ""
          : ((XDevice) trip.device).vehicleIdentificationNumber.replace(",", " "),
        getName(trip.driver),
        Optional.ofNullable(trip.driver)
          .filter(d -> !Iterables.isEmpty(d.keys))
          .map(d -> d.keys.stream().map(k -> k.serialNumber).collect(joining("~")))
          .orElse(""),
        Objects.toString(trip.start, ""),
        Objects.toString(trip.stop, ""),
        Objects.toString(trip.distance, "")
      }).map(array -> stream(array).map(CsvExporter::escapeCsv).toArray(String[]::new))
      .collect(Collectors.toList());
  }

  private String getName(NameEntity entity) {
    return entity.isSystemEntity()
      ? entity.getClass().getSimpleName().replace(",", " ")
      : entity.getName().replace(",", " ");
  }

  public static String escapeCsv(final String input) {
    final String Q = String.valueOf('"');
    return !input.contains(Q) ? input : Q + input.replaceAll(Q, Q + Q) + Q;
  }
}
