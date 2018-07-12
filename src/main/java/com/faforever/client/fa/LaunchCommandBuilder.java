package com.faforever.client.fa;

import com.faforever.client.game.Faction;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import org.springframework.util.StringUtils;

import java.net.Inet4Address;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LaunchCommandBuilder {

  private static final Pattern QUOTED_STRING_PATTERN = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");

  private Float mean;
  private Float deviation;
  private String country;
  private String clan;
  private String nameAva;
  private String tooltipAva;
  private String username;
  private Integer uid;
  private Path executable;
  private List<String> additionalArgs;
  private Integer localGpgPort;
  private Path logFile;
  private Path replayFile;
  private Integer replayId;
  private URI replayUri;
  private Faction faction;
  private String executableDecorator;
  private boolean rehost;
  private Integer localReplayPort;

  private LaunchCommandBuilder() {
    executableDecorator = "\"%s\"";
  }

  public static LaunchCommandBuilder create() {
    return new LaunchCommandBuilder();
  }

  private static List<String> split(String string) {
    Matcher matcher = QUOTED_STRING_PATTERN.matcher(string);
    ArrayList<String> result = new ArrayList<>();
    while (matcher.find()) {
      result.add(matcher.group(1).replace("\"", ""));
    }
    return result;
  }

  public LaunchCommandBuilder localGpgPort(int localGpgPort) {
    this.localGpgPort = localGpgPort;
    return this;
  }

  public LaunchCommandBuilder localReplayPort(int localReplayPort) {
    this.localReplayPort = localReplayPort;
    return this;
  }

  public LaunchCommandBuilder executable(Path executable) {
    this.executable = executable;
    return this;
  }

  public LaunchCommandBuilder uid(Integer uid) {
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

  public LaunchCommandBuilder nameAva(String nameAva){
    this.nameAva = nameAva;
    return this;
  }

  public LaunchCommandBuilder tooltipAva(String tooltipAva){
    this.tooltipAva=tooltipAva;
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

  public LaunchCommandBuilder replayId(Integer replayId) {
    this.replayId = replayId;
    return this;
  }

  public LaunchCommandBuilder replayFile(Path replayFile) {
    this.replayFile = replayFile;
    return this;
  }

  public LaunchCommandBuilder replayUri(URI replayUri) {
    this.replayUri = replayUri;
    return this;
  }

  public LaunchCommandBuilder faction(Faction faction) {
    this.faction = faction;
    return this;
  }

  public LaunchCommandBuilder rehost(boolean rehost) {
    this.rehost = rehost;
    return this;
  }

  public List<String> build() {
    if (executableDecorator == null) {
      throw new IllegalStateException("executableDecorator has not been set");
    }
    if (executable == null) {
      throw new IllegalStateException("executable has not been set");
    }
    if (replayUri != null && uid != null) {
      throw new IllegalStateException("uid and replayUri cannot be set at the same time");
    }
    if (uid != null && username == null) {
      throw new IllegalStateException("username has not been set");
    }


    List<String> command = new ArrayList<>();
    command.addAll(split(String.format(executableDecorator, executable.toAbsolutePath().toString())));
    command.addAll(Arrays.asList(
        "/init", ForgedAlliancePrefs.INIT_FILE_NAME,
        "/nobugreport"
    ));

    if (faction != null) {
      command.add(String.format("/%s", faction.getString()));
    }

    if (logFile != null) {
      command.add("/log");
      command.add(logFile.toAbsolutePath().toString());
    }

    String localIp = Inet4Address.getLoopbackAddress().getHostAddress();
    if (localGpgPort != null) {
      command.add("/gpgnet");
      command.add(localIp + ":" + localGpgPort);
    }


    if(nameAva != null) {
	  String nm = null;
	  nm = Paths.get(URI.create(nameAva).getPath()).getFileName().toString();
      command.add("/avatarurl");
      command.add(String.valueOf(nm)); 

      command.add("/avatartlp");
      command.add(String.valueOf(tooltipAva));
    }

    if (mean != null) {
      command.add("/mean");
      command.add(String.valueOf(mean));
    }

    if (deviation != null) {
      command.add("/deviation");
      command.add(String.valueOf(deviation));
    }

    if (replayFile != null) {
      command.add("/replay");
      command.add(replayFile.toAbsolutePath().toString());
    } else if (replayUri != null) {
      command.add("/replay");
      command.add(replayUri.toASCIIString());
    }

    if (uid != null && localReplayPort != null) {
      command.add("/savereplay");
      command.add("gpgnet://" + localIp + ":" + localReplayPort + "/" + uid + "/" + username + ".SCFAreplay");
    }

    if (country != null) {
      command.add("/country");
      command.add(country);
    }

    if (!StringUtils.isEmpty(clan)) {
      command.add("/clan");
      command.add(clan);
    }

    if (replayId != null) {
      command.add("/replayid");
      command.add(String.valueOf(replayId));
    }

    if (rehost) {
      command.add("/rehost");
    }

    if (additionalArgs != null) {
      command.addAll(additionalArgs);
    }

    return command;
  }

  public LaunchCommandBuilder executableDecorator(String executableDecorator) {
    this.executableDecorator = executableDecorator;
    return this;
  }
}
