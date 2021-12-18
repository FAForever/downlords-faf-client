package com.faforever.client.fa;

import com.faforever.client.domain.LeaderboardRatingBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.commons.lobby.GameLaunchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
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

  private final PlayerService playerService;
  private final PreferencesService preferencesService;
  private final LoggingService loggingService;

  public Process startGameOffline(String map) throws IOException {
    Path executable = getExecutable();
    List<String> launchCommand = LaunchCommandBuilder.create()
        .executableDecorator(preferencesService.getPreferences().getForgedAlliance().getExecutableDecorator())
        .executable(executable)
        .map(map)
        .logFile(loggingService.getNewGameLogFile(0))
        .build();

    return launch(executable, launchCommand);
  }

  public Process startGameOnline(GameLaunchResponse gameLaunchMessage, int gpgPort, int localReplayPort, boolean rehost) throws IOException {
    Path executable = getExecutable();
    PlayerBean currentPlayer = playerService.getCurrentPlayer();

    Optional<LeaderboardRatingBean> leaderboardRating = Optional.of(currentPlayer.getLeaderboardRatings())
        .map(rating -> rating.get(gameLaunchMessage.getLeaderboard()));

    float mean = leaderboardRating.map(LeaderboardRatingBean::getMean).orElse(0f);
    float deviation = leaderboardRating.map(LeaderboardRatingBean::getDeviation).orElse(0f);

    int uid = gameLaunchMessage.getUid();
    List<String> launchCommand = defaultLaunchCommand()
        .executable(executable)
        .uid(uid)
        .faction(gameLaunchMessage.getFaction())
        .mapPosition(gameLaunchMessage.getMapPosition())
        .expectedPlayers(gameLaunchMessage.getExpectedPlayers())
        .team(gameLaunchMessage.getTeam())
        .gameOptions(gameLaunchMessage.getGameOptions())
        .clan(currentPlayer.getClan())
        .country(currentPlayer.getCountry())
        .username(currentPlayer.getUsername())
        .numberOfGames(currentPlayer.getNumberOfGames())
        .deviation(deviation)
        .mean(mean)
        .additionalArgs(gameLaunchMessage.getArgs())
        .logFile(loggingService.getNewGameLogFile(uid))
        .localGpgPort(gpgPort)
        .localReplayPort(localReplayPort)
        .rehost(rehost)
        .build();

    return launch(executable, launchCommand);
  }


  public Process startReplay(Path path, @Nullable Integer replayId) throws IOException {
    Path executable = getExecutable();
    replayId = replayId == null ? -1 : replayId;

    List<String> launchCommand = defaultLaunchCommand()
        .executable(executable)
        .replayFile(path)
        .replayId(replayId)
        .logFile(loggingService.getNewGameLogFile(replayId))
        .build();

    return launch(executable, launchCommand);
  }


  public Process startReplay(URI replayUri, Integer replayId, PlayerBean currentPlayer) throws IOException {
    Path executable = getExecutable();

    List<String> launchCommand = defaultLaunchCommand()
        .executable(executable)
        .replayUri(replayUri)
        .replayId(replayId)
        .logFile(LoggingService.FAF_LOG_DIRECTORY.resolve("replay.log"))
        .username(currentPlayer.getUsername())
        .build();

    return launch(executable, launchCommand);
  }

  private Path getExecutable() {
    return preferencesService.getFafBinDirectory().resolve(FORGED_ALLIANCE_EXE);
  }

  private LaunchCommandBuilder defaultLaunchCommand() {
    return LaunchCommandBuilder.create()
        .executableDecorator(preferencesService.getPreferences().getForgedAlliance().getExecutableDecorator());
  }

  @NotNull
  private Process launch(Path executablePath, List<String> launchCommand) throws IOException {
    Path executeDirectory = preferencesService.getPreferences().getForgedAlliance().getExecutionDirectory();
    if (executeDirectory == null) {
      executeDirectory = executablePath.getParent();
    }

    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.inheritIO();
    processBuilder.directory(executeDirectory.toFile());
    processBuilder.command(launchCommand);

    log.info("Starting Forged Alliance with command: {} in directory: {}", processBuilder.command(), executeDirectory);

    return processBuilder.start();
  }
}
