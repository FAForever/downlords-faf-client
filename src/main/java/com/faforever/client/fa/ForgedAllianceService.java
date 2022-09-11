package com.faforever.client.fa;

import com.faforever.client.domain.LeaderboardRatingBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fa.Kernel32Ex.WindowsPriority;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
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
 * Knows how to starts/stop Forged Alliance with proper parameters. Downloading maps, mods and updates as well as
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
  private final PreferencesService preferencesService;
  private final LoggingService loggingService;

  public Process startGameOffline(String map) throws IOException {
    List<String> launchCommand = defaultLaunchCommand()
        .map(map)
        .logFile(loggingService.getNewGameLogFile(0))
        .build();

    return launch(launchCommand);
  }

  public Process startGameOnline(GameParameters gameParameters) throws IOException {
    PlayerBean currentPlayer = playerService.getCurrentPlayer();

    Optional<LeaderboardRatingBean> leaderboardRating = Optional.of(currentPlayer.getLeaderboardRatings())
        .map(rating -> rating.get(gameParameters.getLeaderboard()));

    float mean = leaderboardRating.map(LeaderboardRatingBean::getMean).orElse(0f);
    float deviation = leaderboardRating.map(LeaderboardRatingBean::getDeviation).orElse(0f);

    int uid = gameParameters.getUid();

    List<String> launchCommand = defaultLaunchCommand()
        .uid(uid)
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
        .additionalArgs(gameParameters.getAdditionalArgs())
        .logFile(loggingService.getNewGameLogFile(uid))
        .localGpgPort(gameParameters.getLocalGpgPort())
        .localReplayPort(gameParameters.getLocalReplayPort())
        .rehost(gameParameters.isRehost())
        .build();

    return launch(launchCommand);
  }


  public Process startReplay(Path path, @Nullable Integer replayId) throws IOException {
    int checkedReplayId = Objects.requireNonNullElse(replayId, -1);

    List<String> launchCommand = defaultLaunchCommand()
        .replayFile(path)
        .replayId(checkedReplayId)
        .logFile(loggingService.getNewGameLogFile(checkedReplayId))
        .build();

    return launch(launchCommand);
  }


  public Process startReplay(URI replayUri, Integer replayId) throws IOException {
    List<String> launchCommand = defaultLaunchCommand()
        .replayUri(replayUri)
        .replayId(replayId)
        .logFile(loggingService.getNewGameLogFile(replayId))
        .username(playerService.getCurrentPlayer().getUsername())
        .build();

    return launch(launchCommand);
  }

  public Path getExecutablePath() {
    return preferencesService.getPreferences().getData().getBinDirectory().resolve(FORGED_ALLIANCE_EXE);
  }

  public Path getDebuggerExecutablePath() {
    return preferencesService.getPreferences().getData().getBinDirectory().resolve(DEBUGGER_EXE);
  }

  private LaunchCommandBuilder defaultLaunchCommand() {
    LaunchCommandBuilder baseCommandBuilder = LaunchCommandBuilder.create()
        .executableDecorator(preferencesService.getPreferences().getForgedAlliance().getExecutableDecorator())
        .executable(getExecutablePath());

    if (preferencesService.getPreferences()
        .getForgedAlliance()
        .isRunFAWithDebugger() && Files.exists(getDebuggerExecutablePath())) {
      baseCommandBuilder = baseCommandBuilder.debuggerExecutable(getDebuggerExecutablePath());
    }

    return baseCommandBuilder;
  }

  @NotNull
  private Process launch(List<String> launchCommand) throws IOException {
    Path executeDirectory = preferencesService.getPreferences().getForgedAlliance().getExecutionDirectory();
    if (executeDirectory == null) {
      executeDirectory = getExecutablePath().getParent();
    }

    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.inheritIO();
    processBuilder.directory(executeDirectory.toFile());
    processBuilder.command(launchCommand);

    log.info("Starting Forged Alliance with command: {} in directory: {}", processBuilder.command(), executeDirectory);

    Process process = processBuilder.start();
    ProcessUtils.setProcessPriority(process, WindowsPriority.HIGH_PRIORITY_CLASS);
    return process;
  }
}
