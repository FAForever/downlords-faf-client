package com.faforever.client.fa;

import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LaunchCommandBuilder {

  private Float mean;
  private Float deviation;
  private String country;
  private String clan;
  private String username;
  private Integer uid;
  private Path executable;
  private List<String> additionalArgs;
  private Integer localGpgPort;
  private Path logFile;

  private LaunchCommandBuilder() {
    // Private
  }

  public LaunchCommandBuilder localGpgPort(int localGpgPort) {
    this.localGpgPort = localGpgPort;
    return this;
  }

  public LaunchCommandBuilder executable(Path executable) {
    this.executable = executable;
    return this;
  }

  public LaunchCommandBuilder uid(int uid) {
    this.uid = uid;
    return this;
  }

  public LaunchCommandBuilder mean(Float mean) {
    this.mean = mean;
    return this;
  }

  public LaunchCommandBuilder deviation(Float deviation) {
    this.deviation = deviation;
    return this;
  }

  public LaunchCommandBuilder country(String country) {
    this.country = country;
    return this;
  }

  public LaunchCommandBuilder clan(String clan) {
    this.clan = clan;
    return this;
  }

  public LaunchCommandBuilder username(String username) {
    this.username = username;
    return this;
  }


  public LaunchCommandBuilder logFile(Path logFile) {
    this.logFile = logFile;
    return this;
  }

  public LaunchCommandBuilder additionalArgs(List<String> additionalArgs) {
    this.additionalArgs = additionalArgs;
    return this;
  }

  public List<String> build() {
    if (executable == null) {
      throw new IllegalStateException("executable has not been set");
    }
    if (mean == null) {
      throw new IllegalStateException("mean has not been set");
    }
    if (deviation == null) {
      throw new IllegalStateException("deviation has not been set");
    }
    if (country == null) {
      throw new IllegalStateException("country has not been set");
    }
    if (uid == null) {
      throw new IllegalStateException("uid has not been set");
    }
    if (username == null) {
      throw new IllegalStateException("username has not been set");
    }
    if (localGpgPort == null) {
      throw new IllegalStateException("localGpgPort has not been set");
    }
    if (logFile == null) {
      throw new IllegalStateException("logFile has not been set");
    }

    List<String> command = new ArrayList<>(Arrays.asList(
        executable.toAbsolutePath().toString(),
        "/mean", String.valueOf(mean),
        "/deviation", String.valueOf(deviation),
        "/country", country,
        "/init", "init_faf.lua",
        "/savereplay", "gpgnet://localhost/" + uid + "/" + username + ".SCFAreplay",
        "/gpgnet", "127.0.0.1:" + localGpgPort,
        "/log", logFile.toAbsolutePath().toString(),
        "/nobugreport"
    ));

    if (!StringUtils.isEmpty(clan)) {
      command.add("/clan");
      command.add(clan);
    }

    if (additionalArgs != null) {
      command.addAll(additionalArgs);
    }

    return command;
  }

  public static LaunchCommandBuilder create() {
    return new LaunchCommandBuilder();
  }
}
