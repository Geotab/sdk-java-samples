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
    public final Credentials credentials;

    public Cmd(Class<?> type, Arg... args) {
      this.type = type;
      this.args.put("server", new Arg("server", false, "The server name (ex. my.geotab.com)"));
      this.args.put("database", new Arg("database", true, "The database name (ex. G560)"));
      this.args.put("username", new Arg("username", true, "The Geotab user name (ex. user@mail.com)"));
      this.args.put("password", new Arg("password", false, "The Geotab password"));
      this.args.put("sessionId", new Arg("sessionId", false, "The Geotab sessionId"));
      Stream.of(args).forEach(arg -> this.args.put(arg.name, arg));
      // Validate required arguments
      for (Arg arg : this.args.values()) {
        arg.value = System.getProperty(arg.name);
        if (!arg.required) continue;
        if (arg.value == null) die("Missing parameter error: " + arg.name);
      }
      // Try to get password from console with echoing disabled
      boolean passwordFromConsole = false;
      var passwordArg = this.args.get("password");
      if (isNullOrEmpty(this.args.get("sessionId").value) && isNullOrEmpty(passwordArg.value)) {
        if (System.console() != null) {
          char[] password = System.console().readPassword("password: ");
          if (password != null && password.length > 0) {
            passwordArg.value = new String(password);
            passwordFromConsole = true;
          }
        }
        if (isNullOrEmpty(passwordArg.value)) die("Missing parameter error: password (or set sessionId instead)");
      }

      // Show configured parameters
      for (Arg arg : this.args.values()) {
        if (passwordFromConsole && arg.name.equals("password")) continue;
        String argV = arg.value;
        if (argV != null) {
          System.out.println(arg.name + ": " + (arg.name.equals("password") ? "***" : argV));
        }
      }
      // Prepare standard API parameters
      server = this.args.get("server").value;
      credentials = Credentials.builder()
          .database(this.args.get("database").value)
          .password(passwordArg.value)
          .userName(this.args.get("username").value)
          .sessionId(this.args.get("sessionId").value)
          .build();
    }

    private void die(String msg) {
      System.out.println(msg);
      System.out.println();
      System.out.println("Usage: java -cp sdk-java-samples.jar [-DparamName=paramValue…] " + type.getName());
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
      var out = new GeotabApi(this.credentials, this.server, DEFAULT_TIMEOUT);
      out.sessionChangeHook = (session) -> System.out.println("New Session ID: " + session);
      return out;
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
