package com.faforever.client.fa;

import com.faforever.client.domain.LeaderboardRatingBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.commons.lobby.Faction;
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
                           String ratingType, int gpgPort, int localReplayPort, boolean rehost, PlayerBean currentPlayer) throws IOException {
    Path executable = getExecutable();

    Optional<LeaderboardRatingBean> leaderboardRating = Optional.of(currentPlayer.getLeaderboardRatings())
        .map(rating -> rating.get(ratingType));

    float mean = leaderboardRating.map(LeaderboardRatingBean::getMean).orElse(0f);
    float deviation = leaderboardRating.map(LeaderboardRatingBean::getDeviation).orElse(0f);

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


  public Process startReplay(URI replayUri, Integer replayId, PlayerBean currentPlayer) throws IOException {
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

    log.info("Starting Forged Alliance with command: {} in directory: {}", processBuilder.command(), executeDirectory);

    return processBuilder.start();
  }
}
