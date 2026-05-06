package com.geotab.sdk;

import com.geotab.sdk.datafeed.DataFeedApp;
import com.geotab.sdk.getcount.GetCountApp;
import com.geotab.sdk.getlogs.GetLogsApp;
import com.geotab.sdk.importdevices.ImportDevicesApp;
import com.geotab.sdk.importgroups.ImportGroupsApp;
import com.geotab.sdk.importusers.ImportUsersApp;
import com.geotab.sdk.maintenance.MaintenanceApp;
import com.geotab.sdk.textmessage.SendTextMessageApp;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class Main {

  @FunctionalInterface
  interface App {
    void run(String[] args) throws Exception;
  }

  private static final String PREFS_FILE = "app.local.properties";
  private static final String LAST_APP_KEY = "lastApp";

  private static final Map<String, App> APPS = new LinkedHashMap<>();

  static {
    APPS.put("getCount", GetCountApp::main);
    APPS.put("getLogs", GetLogsApp::main);
    APPS.put("importUsers", ImportUsersApp::main);
    APPS.put("importDevices", ImportDevicesApp::main);
    APPS.put("importGroups", ImportGroupsApp::main);
    APPS.put("maintenance", MaintenanceApp::main);
    APPS.put("sendTextMessage", SendTextMessageApp::main);
    APPS.put("dataFeed", DataFeedApp::main);
  }

  public static void main(String[] args) throws Exception {
    // -Dapp → first positional arg → interactive
    String name = System.getProperty("app");
    String[] remaining = args;

    if (name == null && args.length > 0) {
      name = args[0];
      remaining = Arrays.copyOfRange(args, 1, args.length);
    }

    if (name == null) {
      name = promptApp();
      remaining = new String[0];
    }

    App app = resolve(name);
    if (app == null) {
      System.out.println("Unknown app: " + name);
      System.out.println("Available: " + String.join(", ", APPS.keySet()));
      System.exit(1);
    }

    saveLastApp(name);
    app.run(remaining);
  }

  private static String promptApp() {
    String last = loadLastApp();
    System.out.println("Available apps:");
    APPS.keySet().forEach(k -> System.out.println("  " + k));

    if (System.console() == null) {
      System.out.println("Usage: mvn exec:java -Dapp=<appName>");
      System.exit(1);
    }

    String prompt = "app" + (last != null ? " [" + last + "]" : "") + ": ";
    String input = System.console().readLine(prompt);
    if (input == null) System.exit(1);
    input = input.trim();

    if (input.isEmpty() && last != null) return last;
    if (input.isEmpty()) {
      System.out.println("No app selected.");
      System.exit(1);
    }
    return input;
  }

  private static App resolve(String name) {
    for (var entry : APPS.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(name)) return entry.getValue();
    }
    return null;
  }

  private static String loadLastApp() {
    var file = new File(PREFS_FILE);
    if (!file.exists()) return null;
    var props = new Properties();
    try (var reader = new FileReader(file)) {
      props.load(reader);
      return props.getProperty(LAST_APP_KEY);
    } catch (Exception e) {
      return null;
    }
  }

  private static void saveLastApp(String name) {
    var props = new Properties();
    props.setProperty(LAST_APP_KEY, name);
    try (var writer = new FileWriter(PREFS_FILE)) {
      props.store(writer, null);
    } catch (Exception e) {
      // non-critical
    }
  }
}
