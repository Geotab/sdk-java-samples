package com.geotab.sdk.datafeed.exporter;

import com.geotab.sdk.datafeed.loader.DataFeedResult;

public interface Exporter {

  static Exporter create(String exportType, String outputPath) {
    if ("csv".equalsIgnoreCase(exportType)) return new CsvExporter(outputPath);
    return new ConsoleExporter();
  }

  void export(DataFeedResult dataFeedResult) throws Exception;
}
