package com.faforever.client.fa;

import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fa.GameParameters.League;
import com.faforever.client.game.error.GameLaunchException;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.player.LeaderboardRating;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.faforever.client.preferences.PreferencesService.FORGED_ALLIANCE_EXE;

/**
 * Knows how to start/stop Forged Alliance with proper parameters. Downloading maps, mods and updates as well as
 * notifying the server about whether the preferences are running or not is <strong>not</strong> this service's
 * responsibility.
 */
@Lazy
@Service
@RequiredArgsConstructor
@Slf4j
public class ForgedAllianceLaunchService {

  public static final String DEBUGGER_EXE = "FAFDebugger.exe";

  private final PlayerService playerService;
  private final LoggingService loggingService;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final DataPrefs dataPrefs;
  private final FafReReleaseService fafReReleaseService;

  public Process launchOfflineGame(String map) {
    LaunchCommandBuilder commandBuilder = LaunchCommandBuilder.create().map(map)
        .logFile(loggingService.getNewGameLogFile(0));

    return launch(commandBuilder, getExecutablePath());
  }

  public Process launchOnlineGame(GameParameters gameParameters, int gpgPort, int replayPort) {
    PlayerInfo currentPlayer = playerService.getCurrentPlayer();

    Optional<LeaderboardRating> leaderboardRating = Optional.ofNullable(currentPlayer.getLeaderboardRatings()).map(
                                                                    rating -> rating.get(gameParameters.leaderboard()));

    double mean = leaderboardRating.map(LeaderboardRating::mean).orElse(0d);
    double deviation = leaderboardRating.map(LeaderboardRating::deviation).orElse(0d);

    int uid = gameParameters.uid();

    LaunchCommandBuilder commandBuilder = LaunchCommandBuilder.create().uid(uid)
        .faction(gameParameters.faction())
        .mapPosition(gameParameters.mapPosition())
        .expectedPlayers(gameParameters.expectedPlayers())
        .team(gameParameters.team())
        .gameOptions(gameParameters.gameOptions())
        .additionalArgs(gameParameters.additionalArgs())
        .clan(currentPlayer.getClan())
        .country(currentPlayer.getCountry())
        .username(currentPlayer.getUsername())
        .numberOfGames(currentPlayer.getNumberOfGames())
        .mean(mean)
        .localGpgPort(gpgPort)
        .localReplayPort(replayPort)
        .deviation(deviation)
        .logFile(loggingService.getNewGameLogFile(uid));

    League league = gameParameters.league();
    if (league != null) {
      commandBuilder.division(league.division()).subdivision(league.subDivision());
    }

    return launch(commandBuilder, getExecutablePath());
  }


  public Process startReplay(Path path, @Nullable Integer replayId) {
    int checkedReplayId = Objects.requireNonNullElse(replayId, -1);

    LaunchCommandBuilder commandBuilder = LaunchCommandBuilder.create().replayFile(path)
        .replayId(checkedReplayId)
        .logFile(loggingService.getNewReplayLogFile(checkedReplayId));

    return launch(commandBuilder, getReplayExecutablePath());
  }


  public Process startReplay(URI replayUri, Integer replayId) {
    LaunchCommandBuilder commandBuilder = LaunchCommandBuilder.create().replayUri(replayUri)
        .replayId(replayId)
        .logFile(loggingService.getNewReplayLogFile(replayId))
        .username(playerService.getCurrentPlayer().getUsername());

    return launch(commandBuilder, getReplayExecutablePath());
  }

  public Path getExecutablePath() {
    return dataPrefs.getBinDirectory().resolve(FORGED_ALLIANCE_EXE);
  }

  public Path getReplayExecutablePath() {
    return dataPrefs.getReplayBinDirectory().resolve(FORGED_ALLIANCE_EXE);
  }

  public Path getDebuggerExecutablePath() {
    return dataPrefs.getBinDirectory().resolve(DEBUGGER_EXE);
  }

  @NotNull
  private Process launch(LaunchCommandBuilder commandBuilder, Path managedExecutable) {
    boolean useFafRe = SystemUtils.IS_OS_WINDOWS && forgedAlliancePrefs.isUseFafRe();
    Path executeDirectory = forgedAlliancePrefs.getExecutionDirectory();
    commandBuilder.executableDecorator(forgedAlliancePrefs.getExecutableDecorator());

    if (!useFafRe) {
      commandBuilder.executable(managedExecutable);
      if (forgedAlliancePrefs.isRunFAWithDebugger() && Files.exists(getDebuggerExecutablePath())) {
        commandBuilder.debuggerExecutable(getDebuggerExecutablePath());
      }
      if (executeDirectory == null) {
        executeDirectory = dataPrefs.getBinDirectory();
      }
    } else {
      commandBuilder.executable(fafReReleaseService.getExecutable());
      Path managedBinDirectory = managedExecutable.getParent();
      if (executeDirectory == null) {
        executeDirectory = managedBinDirectory;
      }
      commandBuilder.initFile(managedBinDirectory.resolve(ForgedAlliancePrefs.INIT_FILE_NAME).toAbsolutePath());
    }

    List<String> launchCommand = commandBuilder.build();
    if (useFafRe) {
      launchCommand.add("/notelemetry");
    }

    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.inheritIO();
    processBuilder.directory(executeDirectory.toFile());
    processBuilder.command(launchCommand);

    log.info("Starting Forged Alliance with command: {} in directory: {}", processBuilder.command(), executeDirectory);

    try {
      return processBuilder.start();
    } catch (IOException exception) {
      throw new GameLaunchException("Error launching game process", exception, "game.start.couldNotStart");
    }
  }
}
