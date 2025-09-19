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

  public Process launchOfflineGame(String map) {
    List<String> launchCommand = defaultLaunchCommand().map(map).logFile(loggingService.getNewGameLogFile(0)).build();

    return launch(launchCommand);
  }

  public Process launchOnlineGame(GameParameters gameParameters, int gpgPort, int replayPort) {
    PlayerInfo currentPlayer = playerService.getCurrentPlayer();

    Optional<LeaderboardRating> leaderboardRating = Optional.of(currentPlayer.getLeaderboardRatings()).map(
                                                                    rating -> rating.get(gameParameters.leaderboard()));

    double mean = leaderboardRating.map(LeaderboardRating::mean).orElse(0d);
    double deviation = leaderboardRating.map(LeaderboardRating::deviation).orElse(0d);

    int uid = gameParameters.uid();

    LaunchCommandBuilder commandBuilder = defaultLaunchCommand().uid(uid)
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

    return launch(commandBuilder.build());
  }


  public Process startReplay(Path path, @Nullable Integer replayId) {
    int checkedReplayId = Objects.requireNonNullElse(replayId, -1);

    List<String> launchCommand = replayLaunchCommand().replayFile(path)
        .replayId(checkedReplayId)
        .logFile(loggingService.getNewReplayLogFile(checkedReplayId))
        .build();

    return launch(launchCommand);
  }


  public Process startReplay(URI replayUri, Integer replayId) {
    List<String> launchCommand = replayLaunchCommand().replayUri(replayUri)
        .replayId(replayId)
        .logFile(loggingService.getNewReplayLogFile(replayId))
        .username(playerService.getCurrentPlayer().getUsername())
        .build();

    return launch(launchCommand);
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

  private LaunchCommandBuilder defaultLaunchCommand() {
    LaunchCommandBuilder baseCommandBuilder = LaunchCommandBuilder.create()
        .executableDecorator(forgedAlliancePrefs.getExecutableDecorator())
        .executable(getExecutablePath());

    return addDebugger(baseCommandBuilder);
  }

  private LaunchCommandBuilder replayLaunchCommand() {
    LaunchCommandBuilder baseCommandBuilder = LaunchCommandBuilder.create()
        .executableDecorator(forgedAlliancePrefs.getExecutableDecorator())
        .executable(getReplayExecutablePath());

    return addDebugger(baseCommandBuilder);
  }

  private LaunchCommandBuilder addDebugger(LaunchCommandBuilder baseCommandBuilder) {
    if (forgedAlliancePrefs.isRunFAWithDebugger() && Files.exists(getDebuggerExecutablePath())) {
      baseCommandBuilder = baseCommandBuilder.debuggerExecutable(getDebuggerExecutablePath());
    }
    return baseCommandBuilder;
  }

  @NotNull
  private Process launch(List<String> launchCommand) {
    Path executeDirectory = forgedAlliancePrefs.getExecutionDirectory();
    if (executeDirectory == null) {
      executeDirectory = getExecutablePath().getParent();
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
