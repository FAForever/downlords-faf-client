package com.faforever.client.fa;

import com.faforever.client.domain.LeaderboardRatingBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.os.OperatingSystem;
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
 * notifying the server about whether the preferences is running or not is <strong>not</strong> this service's
 * responsibility.
 */
@Lazy
@Service
@RequiredArgsConstructor
@Slf4j
public class ForgedAllianceService {

  public static final String DEBUGGER_EXE = "FAFDebugger.exe";

  private final PlayerService playerService;
  private final LoggingService loggingService;
  private final OperatingSystem operatingSystem;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final DataPrefs dataPrefs;

  public Process startGameOffline(String map) throws IOException {
    List<String> launchCommand = defaultLaunchCommand().map(map).logFile(loggingService.getNewGameLogFile(0)).build();

    return launch(launchCommand);
  }

  public Process startGameOnline(GameParameters gameParameters) throws IOException {
    PlayerBean currentPlayer = playerService.getCurrentPlayer();

    Optional<LeaderboardRatingBean> leaderboardRating = Optional.of(currentPlayer.getLeaderboardRatings())
        .map(rating -> rating.get(gameParameters.getLeaderboard()));

    float mean = leaderboardRating.map(LeaderboardRatingBean::getMean).orElse(0f);
    float deviation = leaderboardRating.map(LeaderboardRatingBean::getDeviation).orElse(0f);

    int uid = gameParameters.getUid();

    List<String> launchCommand = defaultLaunchCommand().uid(uid)
        .faction(gameParameters.getFaction())
        .mapPosition(gameParameters.getMapPosition())
        .expectedPlayers(gameParameters.getExpectedPlayers())
        .team(gameParameters.getTeam())
        .gameOptions(gameParameters.getGameOptions())
        .clan(currentPlayer.getClan())
        .country(currentPlayer.getCountry())
        .username(currentPlayer.getUsername())
        .numberOfGames(currentPlayer.getNumberOfGames())
        .mean(mean)
        .deviation(deviation)
        .division(gameParameters.getDivision())
        .subdivision(gameParameters.getSubdivision())
        .additionalArgs(gameParameters.getAdditionalArgs())
        .logFile(loggingService.getNewGameLogFile(uid))
        .localGpgPort(gameParameters.getLocalGpgPort())
        .localReplayPort(gameParameters.getLocalReplayPort())
        .build();

    return launch(launchCommand);
  }


  public Process startReplay(Path path, @Nullable Integer replayId) throws IOException {
    int checkedReplayId = Objects.requireNonNullElse(replayId, -1);

    List<String> launchCommand = replayLaunchCommand().replayFile(path)
        .replayId(checkedReplayId)
        .logFile(loggingService.getNewGameLogFile(checkedReplayId))
        .build();

    return launch(launchCommand);
  }


  public Process startReplay(URI replayUri, Integer replayId) throws IOException {
    List<String> launchCommand = replayLaunchCommand().replayUri(replayUri)
        .replayId(replayId)
        .logFile(loggingService.getNewGameLogFile(replayId))
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
  private Process launch(List<String> launchCommand) throws IOException {
    Path executeDirectory = forgedAlliancePrefs.getExecutionDirectory();
    if (executeDirectory == null) {
      executeDirectory = getExecutablePath().getParent();
    }

    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.inheritIO();
    processBuilder.directory(executeDirectory.toFile());
    processBuilder.command(launchCommand);

    log.info("Starting Forged Alliance with command: {} in directory: {}", processBuilder.command(), executeDirectory);

    Process process = processBuilder.start();
    if (forgedAlliancePrefs.isChangeProcessPriority()) {
      log.info("Increasing process priority");
      operatingSystem.increaseProcessPriority(process);
    }
    return process;
  }
}
