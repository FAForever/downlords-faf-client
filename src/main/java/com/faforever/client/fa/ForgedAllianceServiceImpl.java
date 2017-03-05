package com.faforever.client.fa;

import com.faforever.client.game.Faction;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static com.faforever.client.preferences.PreferencesService.FORGED_ALLIANCE_EXE;

@Lazy
@Service
public class ForgedAllianceServiceImpl implements ForgedAllianceService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final PreferencesService preferencesService;
  private final PlayerService playerService;

  @Inject
  public ForgedAllianceServiceImpl(PreferencesService preferencesService, PlayerService playerService) {
    this.preferencesService = preferencesService;
    this.playerService = playerService;
  }

  @Override
  public Process startGame(int uid, @Nullable Faction faction, @Nullable List<String> additionalArgs, RatingMode ratingMode, int gpgPort, boolean rehost) throws IOException {
    Path executable = getExecutable();

    Player currentPlayer = playerService.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("Player has not been set"));

    float deviation;
    float mean;

    switch (ratingMode) {
      case LADDER_1V1:
        deviation = currentPlayer.getLeaderboardRatingDeviation();
        mean = currentPlayer.getLeaderboardRatingMean();
        break;
      default:
        deviation = currentPlayer.getGlobalRatingDeviation();
        mean = currentPlayer.getGlobalRatingMean();
    }

    List<String> launchCommand = LaunchCommandBuilder.create()
        .executableDecorator(preferencesService.getPreferences().getForgedAlliance().getExecutableDecorator())
        .executable(executable)
        .uid(uid)
        .faction(faction)
        .clan(currentPlayer.getClan())
        .country(currentPlayer.getCountry())
        .deviation(deviation)
        .mean(mean)
        .username(currentPlayer.getUsername())
        .additionalArgs(additionalArgs)
        .logFile(preferencesService.getFafLogDirectory().resolve("game.log"))
        .localGpgPort(gpgPort)
        .rehost(rehost)
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
        .logFile(preferencesService.getFafLogDirectory().resolve("game.log"))
        .build();

    return launch(executable, launchCommand);
  }

  @Override
  public Process startReplay(URI replayUri, Integer replayId) throws IOException {
    Path executable = getExecutable();

    Player currentPlayer = playerService.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("Player has not been set"));
    List<String> launchCommand = LaunchCommandBuilder.create()
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
