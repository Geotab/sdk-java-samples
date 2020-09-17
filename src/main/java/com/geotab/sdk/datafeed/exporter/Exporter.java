package com.geotab.sdk.datafeed.exporter;

import com.geotab.sdk.datafeed.cli.CommandLineArguments;
import com.geotab.sdk.datafeed.loader.DataFeedResult;
import java.util.function.Function;

public interface Exporter {

  Function<CommandLineArguments, Exporter> FACTORY = commandLineArguments -> {
    if ("csv".equalsIgnoreCase(commandLineArguments.getExportType())) {
      return new CsvExporter(commandLineArguments.getOutputPath());
    }

    return new ConsoleExporter();
  };

  void export(DataFeedResult dataFeedResult) throws Exception;
}
