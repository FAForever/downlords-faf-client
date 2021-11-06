package com.faforever.client.fa;

import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.commons.lobby.Faction;
import com.google.common.base.Strings;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.net.Inet4Address;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.faforever.client.util.Assert.checkNullIllegalState;

public class LaunchCommandBuilder {

  private static final Pattern QUOTED_STRING_PATTERN = Pattern.compile("([^\"]\\S*|\"+.+?\"+)\\s*");
  private static final String DEFAULT_EXECUTABLE_DECORATOR = "\"%s\"";

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
  private Path replayFile;
  private Integer replayId;
  private URI replayUri;
  private Faction faction;
  private String executableDecorator;
  private boolean rehost;
  private Integer localReplayPort;
  private Integer numberOfGames;
  private Integer expectedPlayers;
  private Integer mapPosition;
  private Integer team;
  private String map;
  private Map<String, String> gameOptions;

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

  public LaunchCommandBuilder username(String username) {
    this.username = username;
    return this;
  }

  public LaunchCommandBuilder logFile(Path logFile) {
    this.logFile = logFile;
    return this;
  }

  public LaunchCommandBuilder additionalArgs(List<String> additionalArgs) {
    ArrayList<String> cleanedArgs = new ArrayList<>();

    if (additionalArgs != null) {
      for (String arg : additionalArgs) {
        String[] split = arg.split(" ");

        Collections.addAll(cleanedArgs, split);
      }
    }

    this.additionalArgs = cleanedArgs;
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

  public LaunchCommandBuilder numberOfGames(Integer numberOfGames) {
    this.numberOfGames = numberOfGames;
    return this;
  }

  public LaunchCommandBuilder team(Integer team) {
    this.team = team;
    return this;
  }

  public LaunchCommandBuilder expectedPlayers(Integer expectedPlayers) {
    this.expectedPlayers = expectedPlayers;
    return this;
  }

  public LaunchCommandBuilder mapPosition(Integer mapPosition) {
    this.mapPosition = mapPosition;
    return this;
  }

  public LaunchCommandBuilder map(String map) {
    this.map = map;
    return this;
  }

  public LaunchCommandBuilder gameOptions(Map<String, String> gameOptions) {
    this.gameOptions = gameOptions;
    return this;
  }

  public List<String> build() {
    checkNullIllegalState(executableDecorator, "executableDecorator has not been set");
    checkNullIllegalState(executable, "executable has not been set");
    Assert.state(!(replayUri != null && uid != null), "uid and replayUri cannot be set at the same time");
    Assert.state(!(uid != null && username == null), "username has not been set");

    List<String> command = new ArrayList<>();
    command.addAll(split(String.format(executableDecorator, "\"" + executable.toAbsolutePath() + "\"")));
    command.addAll(Arrays.asList(
        "/init", ForgedAlliancePrefs.INIT_FILE_NAME,
        "/nobugreport"
    ));

    if (faction != null) {
      command.add(String.format("/%s", faction.toString().toLowerCase(Locale.ROOT)));
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

    if (StringUtils.hasText(clan)) {
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

    if (numberOfGames != null) {
      command.add("/numgames");
      command.add(String.valueOf(numberOfGames));
    }

    if (team != null) {
      command.add("/team");
      command.add(String.valueOf(team));
    }

    if (expectedPlayers != null) {
      command.add("/players");
      command.add(String.valueOf(expectedPlayers));
    }

    if (mapPosition != null) {
      command.add("/startspot");
      command.add(String.valueOf(mapPosition));
    }

    if (StringUtils.hasText(map)) {
      command.add("/map");
      command.add(map);
    }

    if (gameOptions != null) {
      command.add("/gameoptions");
      gameOptions.entrySet().stream()
          .map(entry -> entry.getKey() + ":" + entry.getValue())
          .forEach(command::add);
    }

    if (additionalArgs != null) {
      command.addAll(additionalArgs);
    }

    return command;
  }

  public LaunchCommandBuilder executableDecorator(String executableDecorator) {
    this.executableDecorator = Strings.isNullOrEmpty(executableDecorator) ? DEFAULT_EXECUTABLE_DECORATOR : executableDecorator;
    return this;
  }
}
