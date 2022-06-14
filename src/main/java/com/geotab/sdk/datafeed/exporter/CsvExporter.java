package com.geotab.sdk.datafeed.exporter;

import static com.geotab.util.DateTimeUtil.localDateTimeToString;
import static com.geotab.util.DateTimeUtil.nowUtcLocalDateTime;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.geotab.model.entity.NameEntity;
import com.geotab.model.entity.device.GoDevice;
import com.geotab.model.entity.diagnostic.DataDiagnostic;
import com.geotab.model.entity.failuremode.NoFailureMode;
import com.geotab.model.entity.faultdata.FaultData;
import com.geotab.model.entity.logrecord.LogRecord;
import com.geotab.model.entity.statusdata.StatusData;
import com.geotab.model.entity.trip.Trip;
import com.geotab.model.entity.user.Driver;
import com.geotab.model.entity.user.Key;
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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvExporter implements Exporter {

  private static final String[] GPS_DATA_HEADER = new String[]{"Vehicle Name",
      "Vehicle Serial Number", "VIN", "Date", "Longitude", "Latitude", "Speed"};

  private static final String GPS_FILE_NAME_PREFIX = "Gps_Data";

  private static final String[] STATUS_DATA_HEADER = new String[]{"Vehicle Name",
      "Vehicle Serial Number", "VIN", "Date", "Diagnostic Name", "Diagnostic Code", "Source Name",
      "Value", "Units"};

  private static final String STATUS_DATA_FILE_NAME_PREFIX = "Status_Data";

  private static final String[] FAULT_DATA_HEADER = new String[]{"Vehicle Name",
      "Vehicle Serial Number", "VIN", "Date", "Diagnostic Name", "Failure Mode Name",
      "Failure Mode Code", "Failure Mode Source", "Controller Name", "Count", "Active",
      "Malfunction Lamp", "Red Stop Lamp", "Amber Warning Lamp", "Protect Lamp", "Dismiss Date",
      "Dismiss User"};

  private static final String FAULT_DATA_FILE_NAME_PREFIX = "Fault_Data";

  private static final String[] TRIP_HEADER = new String[]{
      "VehicleName", "VehicleSerialNumber", "Vin", "Driver Name", "Driver Keys", "Trip Start Time",
      "Trip End Time", "Trip Distance"};

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

    String csvFile = generateCsv(
        GPS_FILE_NAME_PREFIX,
        GPS_DATA_HEADER,
        transformLogRecords(logRecords)
    );

    log.info("LogRecords exported to {}", csvFile);
  }

  private void exportStatusData(List<StatusData> statusData) throws Exception {
    log.debug("Exporting StatusData to csv…");

    String csvFile = generateCsv(
        STATUS_DATA_FILE_NAME_PREFIX,
        STATUS_DATA_HEADER,
        transformStatusData(statusData)
    );

    log.info("StatusData exported to {}", csvFile);
  }

  private void exportFaultData(List<FaultData> faultData) throws Exception {
    log.debug("Exporting FaultData to csv…");

    String csvFile = generateCsv(
        FAULT_DATA_FILE_NAME_PREFIX,
        FAULT_DATA_HEADER,
        transformFaultData(faultData)
    );

    log.info("FaultData exported to {}", csvFile);
  }

  private void exportTrips(List<Trip> trips) throws Exception {
    log.debug("Exporting Trips to csv…");

    String csvFile = generateCsv(
        TRIP_FILE_NAME_PREFIX,
        TRIP_HEADER,
        transformTrips(trips)
    );

    log.info("Trips exported to {}", csvFile);
  }

  private String generateCsv(String fileNamePrefix, String[] headers, List<String[]> csvRows)
      throws Exception {

    String reportFileName = fileNamePrefix + "-"
        + nowUtcLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"))
        + ".csv";
    Path reportFilePath = Paths.get(outputPath + File.separator + reportFileName);

    boolean addHeaderRow = !Files.exists(reportFilePath);

    try (Writer writer = new FileWriter(reportFilePath.toString(), true)) {
      if (addHeaderRow) {
        writer.append(String.join(",", headers));
      }

      for (String[] row : csvRows) {
        writer.append(System.getProperty("line.separator")).append(String.join(",", row));
      }
    }

    return reportFilePath.toString();
  }

  private List<String[]> transformLogRecords(List<LogRecord> logRecords) {
    if (Iterables.isEmpty(logRecords)) {
      return new ArrayList<>();
    }

    return logRecords.stream()
        .map(logRecord -> stream(
                new String[]{
                    logRecord.getDevice().getName().replace(",", " "),
                    logRecord.getDevice().getSerialNumber(),
                    logRecord.getDevice() instanceof GoDevice
                        ? ((GoDevice) logRecord.getDevice()).getVehicleIdentificationNumber()
                        .replace(",", " ") : "",
                    localDateTimeToString(logRecord.getDateTime()),
                    logRecord.getLongitude() != null ? logRecord.getLongitude().toString() : "",
                    logRecord.getLatitude() != null ? logRecord.getLatitude().toString() : "",
                    logRecord.getSpeed() != null ? logRecord.getSpeed().toString() : "",
                }
            )
                .map(CsvExporter::escapeCsv)
                .toArray(String[]::new)
        )
        .collect(Collectors.toList());
  }

  private List<String[]> transformStatusData(List<StatusData> statusData) {
    if (Iterables.isEmpty(statusData)) {
      return new ArrayList<>();
    }

    return statusData.stream()
        .map(data -> stream(
                new String[]{
                    data.getDevice().getName().replace(",", " "),
                    data.getDevice().getSerialNumber(),
                    data.getDevice() instanceof GoDevice
                        ? ((GoDevice) data.getDevice()).getVehicleIdentificationNumber()
                        .replace(",", " ") : "",
                    localDateTimeToString(data.getDateTime()),
                    getName(data.getDiagnostic()),
                    data.getDiagnostic().getCode() != null ? data.getDiagnostic().getCode()
                        .toString() : "",
                    data.getDiagnostic().getSource() != null ? getName(
                        data.getDiagnostic().getSource()) : "",
                    data.getData() != null ? data.getData().toString() : "",
                    data.getDiagnostic() instanceof DataDiagnostic ? getName(
                        data.getDiagnostic().getUnitOfMeasure()) : ""
                }
            )
                .map(CsvExporter::escapeCsv)
                .toArray(String[]::new)
        )
        .collect(Collectors.toList());
  }

  private List<String[]> transformFaultData(List<FaultData> faultData) {
    if (Iterables.isEmpty(faultData)) {
      return new ArrayList<>();
    }

    return faultData.stream()
        .map(data -> stream(
                new String[]{
                    data.getDevice().getName().replace(",", " "),
                    data.getDevice().getSerialNumber(),
                    data.getDevice() instanceof GoDevice
                        ? ((GoDevice) data.getDevice()).getVehicleIdentificationNumber()
                        .replace(",", " ") : "",
                    localDateTimeToString(data.getDateTime()),
                    getName(data.getDiagnostic()),
                    getName(data.getFailureMode()),
                    data.getFailureMode().getCode() != null ? data.getFailureMode().getCode()
                        .toString() : "",
                    NoFailureMode.getInstance().equals(data.getFailureMode()) ? "None"
                        : getName(data.getFailureMode().getSource()),
                    getName(data.getController()),
                    data.getCount() != null ? data.getCount().toString() : "",
                    data.getFaultState() != null ? data.getFaultState().toString() : "",
                    data.getMalfunctionLamp() != null ? data.getMalfunctionLamp().toString() : "",
                    data.getRedStopLamp() != null ? data.getRedStopLamp().toString() : "",
                    data.getAmberWarningLamp() != null ? data.getAmberWarningLamp().toString() : "",
                    data.getProtectWarningLamp() != null ? data.getProtectWarningLamp().toString()
                        : "",
                    data.getDismissDateTime() != null ? localDateTimeToString(
                        data.getDismissDateTime()) : "",
                    data.getDismissUser() != null ? data.getDismissUser().getName()
                        .replace(",", " ") : ""
                }
            )
                .map(CsvExporter::escapeCsv)
                .toArray(String[]::new)
        )
        .collect(Collectors.toList());
  }

  private List<String[]> transformTrips(List<Trip> trips) {
    if (Iterables.isEmpty(trips)) {
      return new ArrayList<>();
    }

    return trips.stream().map(trip -> new String[]{
        trip.getDevice().getName().replace(",", " "),
        trip.getDevice().getSerialNumber(),
        !(trip.getDevice() instanceof GoDevice) ? ""
            : ((GoDevice) trip.getDevice()).getVehicleIdentificationNumber().replace(",", " "),
        getName(trip.getDriver()),
        trip.getDriver() instanceof Driver && Iterables.isEmpty(((Driver) trip.getDriver()).getKeys()) ? ""
            : ((Driver) trip.getDriver()).getKeys().stream().map(Key::getSerialNumber).collect(joining("~")),
        trip.getStart() != null ? localDateTimeToString(trip.getStart()) : "",
        trip.getStop() != null ? localDateTimeToString(trip.getStop()) : "",
        trip.getDistance() != null ? trip.getDistance().toString() : ""
    }).map(array -> stream(array).map(CsvExporter::escapeCsv).toArray(String[]::new)).collect(Collectors.toList());
  }

  private String getName(NameEntity entity) {
    return entity.isSystemEntity() ? entity.getClass().getSimpleName().replace(",", " ")
        : entity.getName().replace(",", " ");
  }

  public static String escapeCsv(final String input) {
    final String Q = String.valueOf('"');
    return !input.contains(Q) ? input : Q + input.replaceAll(Q, Q + Q) + Q;
  }

}
