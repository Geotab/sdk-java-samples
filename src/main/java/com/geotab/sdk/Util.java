package com.geotab.sdk;

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
      this.args.put("password", new Arg("password", true, "The Geotab password"));
      Stream.of(args).forEach(arg -> this.args.put(arg.name, arg));
      // Validate required arguments
      boolean passwordFromConsole = false;
      for (Arg arg : this.args.values()) {
        arg.value = System.getProperty(arg.name);
        if (!arg.required) continue;
        if (arg.value == null) {
          // Try to get password from console with echoing disabled
          if (arg.name.equals("password") && System.console() != null) {
            char[] password = System.console().readPassword("password: ");
            if (password != null && password.length > 0) {
              arg.value = new String(password);
              passwordFromConsole = true;
              continue;
            }
          }
          die("Missing parameter error: " + arg.name);
        }
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
          .password(this.args.get("password").value)
          .userName(this.args.get("username").value)
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
