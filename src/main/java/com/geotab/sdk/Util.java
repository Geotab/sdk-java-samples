package com.geotab.sdk;

import static com.geotab.http.invoker.ServerInvoker.DEFAULT_TIMEOUT;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.geotab.api.GeotabApi;
import com.geotab.model.login.Credentials;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public interface Util {

  class Cmd {

    private final Class<?> type;
    private final Map<String, Arg> args = new LinkedHashMap<>();
    public final String server;
    public Credentials credentials;

    public Cmd(Class<?> type, Arg... extraArgs) {
      this.type = type;
      this.args.put("server", new Arg("server", false, "The server name (ex. my.geotab.com)"));
      this.args.put("database", new Arg("database", true, "The database name (ex. G560)"));
      this.args.put("username", new Arg("username", true, "The Geotab user name (ex. user@mail.com)"));
      this.args.put("password", new Arg("password", false, "The Geotab password"));
      Stream.of(extraArgs).forEach(arg -> this.args.put(arg.name, arg));

      // Priority 1: system properties (-Dserver=... -Ddatabase=... etc.)
      for (Arg arg : this.args.values()) {
        arg.value = System.getProperty(arg.name);
      }

      // Priority 2: environment variables (GEOTAB_SERVER, GEOTAB_DATABASE, GEOTAB_USER, GEOTAB_PASSWORD)
      fillFromEnv("server",   "GEOTAB_SERVER");
      fillFromEnv("database", "GEOTAB_DATABASE");
      fillFromEnv("username", "GEOTAB_USER");
      fillFromEnv("password", "GEOTAB_PASSWORD");

      // Priority 3: interactive prompts for missing args (all except password)
      for (Arg arg : this.args.values()) {
        if (arg.name.equals("password")) continue;
        if (!isNullOrEmpty(arg.value)) continue;
        if (System.console() != null) {
          String label = arg.name + (arg.required ? "" : " (optional)") + ": ";
          String raw = System.console().readLine(label);
          String input = raw != null ? raw.trim() : "";
          if (!input.isEmpty()) arg.value = input;
        }
        if (arg.required && isNullOrEmpty(arg.value)) die("Missing parameter error: " + arg.name);
      }

      // Password: prompt if not already set via system property or env var
      var passwordArg = this.args.get("password");
      if (isNullOrEmpty(passwordArg.value)) {
        if (System.console() != null) {
          char[] pw = System.console().readPassword("password: ");
          if (pw != null && pw.length > 0) passwordArg.value = new String(pw);
        }
        if (isNullOrEmpty(passwordArg.value)) {
          die("Missing parameter error: password (or set GEOTAB_PASSWORD)");
        }
      }

      // Echo configured parameters — password always masked
      for (Arg arg : this.args.values()) {
        String v = arg.value;
        if (v == null) continue;
        System.out.println(arg.name + ": " + (arg.name.equals("password") ? "***" : v));
      }

      server = this.args.get("server").value;
      credentials = Credentials.builder()
        .database(this.args.get("database").value)
        .userName(this.args.get("username").value)
        .password(passwordArg.value)
        .build();
    }

    private void fillFromEnv(String argName, String envVar) {
      Arg arg = this.args.get(argName);
      if (arg != null && isNullOrEmpty(arg.value)) {
        String v = System.getenv(envVar);
        if (!isNullOrEmpty(v)) arg.value = v;
      }
    }

    private void die(String msg) {
      System.out.println(msg);
      System.out.println();
      System.out.println("Usage: mvn exec:java -Dapp=<name> [-DparamName=value…]");
      System.out.println("  or set environment variables: GEOTAB_SERVER, GEOTAB_DATABASE, GEOTAB_USER, GEOTAB_PASSWORD");
      System.out.println("Parameters:");
      for (Arg arg : args.values()) {
        System.out.println("  " + arg.name + " (" + (arg.required ? "required" : "optional") + "): " + arg.description);
      }
      System.exit(1);
    }

    public String get(String name) {
      if (!args.containsKey(name)) throw new RuntimeException("unregistered param error");
      return args.get(name).value;
    }

    public GeotabApi newApi() {
      GeotabApi api = new GeotabApi(this.credentials, this.server, DEFAULT_TIMEOUT);
      api.authenticate();
      return api;
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
