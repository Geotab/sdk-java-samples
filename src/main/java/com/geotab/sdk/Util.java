package com.geotab.sdk;

import static com.geotab.http.invoker.ServerInvoker.DEFAULT_TIMEOUT;
import static com.geotab.plain.Entities.UserEntity;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.geotab.api.GeotabApi;
import com.geotab.http.exception.InvalidUserException;
import com.geotab.model.login.Credentials;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

public interface Util {

  class Cmd {

    private static final String SESSION_FILE = "session.local.properties";

    private final Class<?> type;
    private final Map<String, Arg> args = new LinkedHashMap<>();
    public final String server;
    public Credentials credentials;
    private boolean sessionLoaded;

    public Cmd(Class<?> type, Arg... extraArgs) {
      this.type = type;
      this.args.put("server", new Arg("server", false, "The server name (ex. my.geotab.com)"));
      this.args.put("database", new Arg("database", true, "The database name (ex. G560)"));
      this.args.put("username", new Arg("username", true, "The Geotab user name (ex. user@mail.com)"));
      this.args.put("password", new Arg("password", false, "The Geotab password"));
      this.args.put("sessionId", new Arg("sessionId", false, "The Geotab sessionId"));
      Stream.of(extraArgs).forEach(arg -> this.args.put(arg.name, arg));

      // System properties
      for (Arg arg : this.args.values()) {
        arg.value = System.getProperty(arg.name);
      }

      // Session file fills gaps (system props take precedence)
      Properties session = loadSessionFile();
      if (session != null) {
        for (String key : new String[] { "server", "database", "username", "sessionId" }) {
          Arg arg = this.args.get(key);
          if (arg != null && isNullOrEmpty(arg.value)) {
            arg.value = session.getProperty(key);
          }
        }
        sessionLoaded = !isNullOrEmpty(this.args.get("sessionId").value);
      }

      // Interactive prompts for missing args (required + optional, except password/sessionId)
      for (Arg arg : this.args.values()) {
        if (arg.name.equals("password") || arg.name.equals("sessionId")) continue;
        if (!isNullOrEmpty(arg.value)) continue;
        if (System.console() != null) {
          String label = arg.name + (arg.required ? "" : " (optional)") + ": ";
          String raw = System.console().readLine(label);
          String input = raw != null ? raw.trim() : "";
          if (!input.isEmpty()) arg.value = input;
        }
        if (arg.required && isNullOrEmpty(arg.value)) die("Missing parameter error: " + arg.name);
      }

      // Password prompt if no session
      var passwordArg = this.args.get("password");
      if (!sessionLoaded && isNullOrEmpty(passwordArg.value)) {
        if (System.console() != null) {
          char[] pw = System.console().readPassword("password: ");
          if (pw != null && pw.length > 0) passwordArg.value = new String(pw);
        }
        if (isNullOrEmpty(passwordArg.value)) {
          die("Missing parameter error: password (or set sessionId instead)");
        }
      }

      // Show configured parameters
      for (Arg arg : this.args.values()) {
        String v = arg.value;
        if (v == null) continue;
        boolean sensitive = arg.name.equals("password") || arg.name.equals("sessionId");
        System.out.println(arg.name + ": " + (sensitive ? "***" : v));
      }

      server = this.args.get("server").value;
      if (sessionLoaded) {
        credentials = Credentials.builder()
          .database(this.args.get("database").value)
          .userName(this.args.get("username").value)
          .sessionId(this.args.get("sessionId").value)
          .build();
      } else {
        credentials = Credentials.builder()
          .database(this.args.get("database").value)
          .userName(this.args.get("username").value)
          .password(passwordArg.value)
          .build();
      }
    }

    private void die(String msg) {
      System.out.println(msg);
      System.out.println();
      System.out.println("Usage: mvn exec:java -Dapp=<name> [-DparamName=value…]");
      System.out.println("Parameters:");
      for (Arg arg : args.values()) {
        System.out.println(arg.name + " (" + (arg.required ? "required" : "optional") + "): " + arg.description);
      }
      System.exit(1);
    }

    public String get(String name) {
      if (!args.containsKey(name)) throw new RuntimeException("unregistered param error");
      return args.get(name).value;
    }

    public GeotabApi newApi() {
      GeotabApi api = new GeotabApi(this.credentials, this.server, DEFAULT_TIMEOUT);
      setupSessionHook(api);

      if (sessionLoaded) {
        try {
          api.callGetCountOf(UserEntity, null);
          return api;
        } catch (InvalidUserException e) {
          System.out.println("Session expired, re-authenticating...");
          deleteSessionFile();
          sessionLoaded = false;
          String password = promptPassword();
          if (isNullOrEmpty(password)) throw new RuntimeException("password required for re-authentication");
          credentials = Credentials.builder()
            .database(this.args.get("database").value)
            .userName(this.args.get("username").value)
            .password(password)
            .build();
          api = new GeotabApi(this.credentials, this.server, DEFAULT_TIMEOUT);
          setupSessionHook(api);
        }
      }

      api.authenticate();
      return api;
    }

    private void setupSessionHook(GeotabApi api) {
      api.sessionChangeHook = session -> {
        System.out.println("New Session ID: " + session);
        saveSessionFile(
          this.args.get("server").value,
          this.args.get("database").value,
          this.args.get("username").value,
          session);
      };
    }

    private String promptPassword() {
      if (System.console() != null) {
        char[] pw = System.console().readPassword("password: ");
        if (pw != null && pw.length > 0) return new String(pw);
      }
      return null;
    }

    private static Properties loadSessionFile() {
      var file = new File(SESSION_FILE);
      if (!file.exists()) return null;
      var props = new Properties();
      try (var reader = new FileReader(file)) {
        props.load(reader);
        return props;
      } catch (Exception e) {
        return null;
      }
    }

    private static void saveSessionFile(
      String server, String database, String username, String sessionId) {
      var props = new Properties();
      if (!isNullOrEmpty(server)) props.setProperty("server", server);
      props.setProperty("database", database);
      props.setProperty("username", username);
      props.setProperty("sessionId", sessionId);
      try (var writer = new FileWriter(SESSION_FILE)) {
        props.store(writer, "Geotab session — do not commit");
      } catch (Exception e) {
        System.out.println("Warning: could not save session file: " + e.getMessage());
      }
    }

    private static void deleteSessionFile() {
      new File(SESSION_FILE).delete();
    }
  }

  class Arg {

    public final String name;
    public final boolean required;
    public final String description;
    public String value;

    public Arg(String name, boolean required, String description) {
      this.name = name;
      this.required = required;
      this.description = description;
    }
  }
}
