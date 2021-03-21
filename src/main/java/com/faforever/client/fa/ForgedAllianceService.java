package com.faforever.client.fa;

import com.faforever.client.game.Faction;
import com.faforever.client.leaderboard.LeaderboardRating;
import com.faforever.client.player.Player;
import com.faforever.client.preferences.PreferencesService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
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
public class ForgedAllianceService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final PreferencesService preferencesService;

  public Process startGameOffline(List<String> args) throws IOException {
    Path executable = getExecutable();
    List<String> launchCommand = LaunchCommandBuilder.create()
        .executableDecorator(preferencesService.getPreferences().getForgedAlliance().getExecutableDecorator())
        .executable(executable)
        .additionalArgs(args)
        .logFile(preferencesService.getNewGameLogFile(0))

        .build();

    return launch(executable, launchCommand);
  }

  public Process startGame(int uid, @Nullable Faction faction, @Nullable List<String> additionalArgs,
                           String ratingType, int gpgPort, int localReplayPort, boolean rehost, Player currentPlayer) throws IOException {
    Path executable = getExecutable();

    Optional<LeaderboardRating> leaderboardRating = Optional.of(currentPlayer.getLeaderboardRatings())
        .map(rating -> rating.get(ratingType));

    float mean = leaderboardRating.map(LeaderboardRating::getMean).orElse(0f);
    float deviation = leaderboardRating.map(LeaderboardRating::getDeviation).orElse(0f);

    List<String> launchCommand = defaultLaunchCommand()
        .executable(executable)
        .uid(uid)
        .faction(faction)
        .clan(currentPlayer.getClan())
        .country(currentPlayer.getCountry())
        .deviation(deviation)
        .mean(mean)
        .username(currentPlayer.getUsername())
        .additionalArgs(additionalArgs)
        .logFile(preferencesService.getNewGameLogFile(uid))
        .localGpgPort(gpgPort)
        .localReplayPort(localReplayPort)
        .rehost(rehost)
        .build();

    return launch(executable, launchCommand);
  }


  public Process startReplay(Path path, @Nullable Integer replayId) throws IOException {
    Path executable = getExecutable();

    List<String> launchCommand = defaultLaunchCommand()
        .executable(executable)
        .replayFile(path)
        .replayId(replayId)
        .logFile(preferencesService.getNewGameLogFile(replayId))
        .build();

    return launch(executable, launchCommand);
  }


  public Process startReplay(URI replayUri, Integer replayId, Player currentPlayer) throws IOException {
    Path executable = getExecutable();

    List<String> launchCommand = defaultLaunchCommand()
        .executable(executable)
        .replayUri(replayUri)
        .replayId(replayId)
        .logFile(preferencesService.getFafLogDirectory().resolve("replay.log"))
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

    logger.info("Starting Forged Alliance with command: {} in directory: {}", processBuilder.command(), executeDirectory);

    return processBuilder.start();
  }
}
