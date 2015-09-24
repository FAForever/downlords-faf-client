package com.faforever.client.fa;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.game.Faction;
import com.faforever.client.legacy.relay.LocalRelayServer;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public class ForgedAllianceServiceImpl implements ForgedAllianceService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  PlayerService playerService;

  @Autowired
  LocalRelayServer localRelayServer;

  @Override
  public Process startGame(int uid, @NotNull String gameType, @Nullable Faction faction, @Nullable List<String> additionalArgs, RatingMode ratingMode) throws IOException {
    Path executable = getExecutable();

    PlayerInfoBean currentPlayer = playerService.getCurrentPlayer();

    float deviation;
    float mean;

    switch (ratingMode) {
      case RANKED_1V1:
        deviation = currentPlayer.getLeaderboardRatingDeviation();
        mean = currentPlayer.getLeaderboardRatingMean();
        break;
      default:
        deviation = currentPlayer.getGlobalRatingDeviation();
        mean = currentPlayer.getGlobalRatingMean();
    }

    List<String> launchCommand = LaunchCommandBuilder.create()
        .executable(executable)
        .gameType(gameType)
        .uid(uid)
        .faction(faction)
        .clan(currentPlayer.getClan())
        .country(currentPlayer.getCountry())
        .deviation(deviation)
        .mean(mean)
        .username(currentPlayer.getUsername())
        .additionalArgs(additionalArgs)
        .logFile(preferencesService.getFafLogDirectory().resolve("game.log"))
        .localGpgPort(localRelayServer.getPort())
        .build();

    return launch(executable, launchCommand);
  }

  @Override
  public Process startReplay(Path path, @Nullable Integer replayId) throws IOException {
    Path executable = getExecutable();

    List<String> launchCommand = LaunchCommandBuilder.create()
        .executable(executable)
        .replayFile(path)
        .replayId(replayId)
            // FIXME fix the path
        .logFile(preferencesService.getFafDataDirectory().resolve("logs/replay.log"))
        .build();

    return launch(executable, launchCommand);
  }

  @Override
  public Process startReplay(URL replayUrl, Integer replayId) throws IOException {
    Path executable = getExecutable();

    List<String> launchCommand = LaunchCommandBuilder.create()
        .executable(executable)
        .replayUrl(replayUrl)
        .replayId(replayId)
            // FIXME fix the path
        .logFile(preferencesService.getFafDataDirectory().resolve("logs/replay.log"))
        .build();

    return launch(executable, launchCommand);
  }

  private Path getExecutable() {
    return preferencesService.getFafBinDirectory().resolve("ForgedAlliance.exe");
  }

  @NotNull
  private Process launch(Path executable, List<String> launchCommand) throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.inheritIO();
    processBuilder.directory(executable.getParent().toAbsolutePath().toFile());
    processBuilder.command(launchCommand);

    logger.info("Starting Forged Alliance with command: " + processBuilder.command());

    return processBuilder.start();
  }
}
