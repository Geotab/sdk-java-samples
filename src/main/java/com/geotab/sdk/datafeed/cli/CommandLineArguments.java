package com.geotab.sdk.datafeed.cli;

import com.geotab.model.login.Credentials;
import com.geotab.sdk.datafeed.loader.DataFeedParameters;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CommandLineArguments {

  private static final String SERVER_ARG_NAME = "s";
  private static final String DATABASE_ARG_NAME = "d";
  private static final String USER_ARG_NAME = "u";
  private static final String PASSWORD_ARG_NAME = "p";
  private static final String GPS_TOKEN_ARG_NAME = "gt";
  private static final String STATUS_DATA_TOKEN_ARG_NAME = "st";
  private static final String FAULT_DATA_TOKEN_ARG_NAME = "ft";
  private static final String TRIP_TOKEN_ARG_NAME = "tt";
  private static final String EXCEPTION_TOKEN_ARG_NAME = "et";
  private static final String EXPORT_TYPE_ARG_NAME = "exp";
  private static final String OUTPUT_FOLDER_ARG_NAME = "f";
  private static final String FEED_CONTINUOUSLY_ARG_NAME = "c";

  private String server;
  private Credentials credentials;
  private DataFeedParameters dataFeedParameters;
  private String exportType;
  private String outputPath;
  private boolean feedContinuously;

  public CommandLineArguments(String[] args) throws ParseException {
    parseArguments(args);
  }

  private void parseArguments(String[] args) throws ParseException {
    Options options = configureOptions();

    CommandLineParser parser = new DataFeedCommandLineParser();
    CommandLine commandLine = parser.parse(options, args);

    if (!commandLine.hasOption(SERVER_ARG_NAME)
        || !commandLine.hasOption(DATABASE_ARG_NAME)
        || !commandLine.hasOption(USER_ARG_NAME)
        || !commandLine.hasOption(PASSWORD_ARG_NAME)) {
      String passedParams = String.join(" ", args);
      String cmdLineSyntax =
          "java -cp sdk-java-samples.jar com.geotab.sdk.datafeed.DataFeedApp"
              + " --s server --d database --u user --p password --gt nnn --st nnn --ft nnn --tt nnn"
              + " --et nnn --exp csv --f file path --c";
      String header = "\n\tPassed params: " + passedParams
          + "\n\tArguments may be in any order: ";
      String footer = "";
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(cmdLineSyntax, header, options, footer, true);

      throw new MissingArgumentException("\nMissing required parameters; check application usage.");
    }

    extractValues(commandLine);
  }

  @SuppressWarnings("Indentation")
  private Options configureOptions() {
    Options options = new Options();

    options
        .addOption(Option.builder(SERVER_ARG_NAME)
            .argName("server")
            .optionalArg(false)
            .hasArg(true)
            .desc("[required] The Server")
            .build()
        )
        .addOption(Option.builder(DATABASE_ARG_NAME)
            .argName("database")
            .optionalArg(false)
            .hasArg(true)
            .desc("[required] The Database")
            .build()
        )
        .addOption(Option.builder(USER_ARG_NAME)
            .argName("user")
            .optionalArg(false)
            .hasArg(true)
            .desc("[required] The User")
            .build()
        )
        .addOption(Option.builder(PASSWORD_ARG_NAME)
            .argName("password")
            .optionalArg(false)
            .hasArg(true)
            .desc("[required] The Password")
            .build()
        )
        .addOption(Option.builder(GPS_TOKEN_ARG_NAME)
            .argName("gpsToken")
            .optionalArg(true)
            .hasArg(true)
            .desc("[optional] The last known gps data token")
            .build()
        )
        .addOption(Option.builder(STATUS_DATA_TOKEN_ARG_NAME)
            .argName("statusToken")
            .optionalArg(true)
            .hasArg(true)
            .desc("[optional] The last known status data token")
            .build()
        )
        .addOption(Option.builder(FAULT_DATA_TOKEN_ARG_NAME)
            .argName("faultDataToken")
            .optionalArg(true)
            .hasArg(true)
            .desc("[optional] The last known fault data token")
            .build()
        )
        .addOption(Option.builder(TRIP_TOKEN_ARG_NAME)
            .argName("tripToken")
            .optionalArg(true)
            .hasArg(true)
            .desc("[optional] The last known trip token")
            .build()
        )
        .addOption(Option.builder(EXCEPTION_TOKEN_ARG_NAME)
            .argName("exceptionToken")
            .optionalArg(true)
            .hasArg(true)
            .desc("[optional] The last known exception token")
            .build()
        )
        .addOption(Option.builder(EXPORT_TYPE_ARG_NAME)
            .argName("exportType")
            .optionalArg(true)
            .hasArg(true)
            .desc(
                "[optional] The export type: console, csv. Defaults to console.")
            .build()
        )
        .addOption(Option.builder(OUTPUT_FOLDER_ARG_NAME)
            .argName("outputFolder")
            .optionalArg(true)
            .hasArg(true)
            .desc(
                "[optional] The folder to save any output files to, if applicable. "
                    + "Defaults to the current directory.")
            .build()
        )
        .addOption(Option.builder(FEED_CONTINUOUSLY_ARG_NAME)
            .argName("feedContinuously")
            .optionalArg(true)
            .hasArg(true)
            .desc("[optional] Run the feed continuously. Defaults to false.")
            .build()
        );

    return options;
  }

  private void extractValues(CommandLine cl) {
    this.server = cl.getOptionValue(SERVER_ARG_NAME);
    this.credentials = Credentials.builder()
        .database(cl.getOptionValue(DATABASE_ARG_NAME))
        .userName(cl.getOptionValue(USER_ARG_NAME))
        .password(cl.getOptionValue(PASSWORD_ARG_NAME))
        .build();
    this.dataFeedParameters = new DataFeedParameters();
    DataFeedParameters f = this.dataFeedParameters;
    if (cl.hasOption(GPS_TOKEN_ARG_NAME)) f.lastGpsDataToken = cl.getOptionValue(GPS_TOKEN_ARG_NAME);
    if (cl.hasOption(STATUS_DATA_TOKEN_ARG_NAME)) f.lastStatusDataToken = cl.getOptionValue(STATUS_DATA_TOKEN_ARG_NAME);
    if (cl.hasOption(FAULT_DATA_TOKEN_ARG_NAME)) f.lastFaultDataToken = cl.getOptionValue(FAULT_DATA_TOKEN_ARG_NAME);
    if (cl.hasOption(TRIP_TOKEN_ARG_NAME)) f.lastTripToken = cl.getOptionValue(TRIP_TOKEN_ARG_NAME);
    if (cl.hasOption(EXCEPTION_TOKEN_ARG_NAME)) f.lastExceptionToken = cl.getOptionValue(EXCEPTION_TOKEN_ARG_NAME);
    this.exportType = cl.getOptionValue(EXPORT_TYPE_ARG_NAME);
    this.outputPath = cl.getOptionValue(OUTPUT_FOLDER_ARG_NAME);
    this.feedContinuously = cl.hasOption(FEED_CONTINUOUSLY_ARG_NAME)
        && Boolean.parseBoolean(cl.getOptionValue(FEED_CONTINUOUSLY_ARG_NAME));
  }

  public String getServer() {
    return this.server;
  }

  public Credentials getCredentials() {
    return this.credentials;
  }

  public DataFeedParameters getDataFeedParameters() {
    return this.dataFeedParameters;
  }

  public String getExportType() {
    return this.exportType;
  }

  public String getOutputPath() {
    return this.outputPath;
  }

  public boolean isFeedContinuously() {
    return this.feedContinuously;
  }
}
