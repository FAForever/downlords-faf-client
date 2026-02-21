package com.faforever.client.fa;

import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fa.GameParameters.League;
import com.faforever.client.game.error.GameLaunchException;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.player.LeaderboardRating;
import com.faforever.client.player.PlayerService;
import com.faforever.client.fa.rendering.RenderingWrapperService;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.RenderingBackend;
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
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.faforever.client.preferences.PreferencesService.FORGED_ALLIANCE_EXE;

/**
 * Knows how to start/stop Forged Alliance with proper parameters. Downloading
 * maps, mods and updates as well as
 * notifying the server about whether the preferences are running or not is
 * <strong>not</strong> this service's
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
  private final RenderingWrapperService renderingWrapperService;

  public Process launchOfflineGame(String map) {
    List<String> launchCommand = defaultLaunchCommand().map(map).logFile(loggingService.getNewGameLogFile(0)).build();

    return launch(launchCommand);
  }

  public Process launchOnlineGame(GameParameters gameParameters, int gpgPort, int replayPort) {
    PlayerInfo currentPlayer = playerService.getCurrentPlayer();

    Optional<LeaderboardRating> leaderboardRating = Optional.ofNullable(currentPlayer.getLeaderboardRatings()).map(
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

    Set<Path> injectedFiles = injectRenderingWrapper(executeDirectory);

    try {
      Process process = processBuilder.start();
      process.onExit().whenCompleteAsync((p, e) -> removeRenderingWrapper(injectedFiles));
      return process;
    } catch (IOException exception) {
      removeRenderingWrapper(injectedFiles);
      throw new GameLaunchException("Error launching game process", exception, "game.start.couldNotStart");
    }
  }

  private Set<Path> injectRenderingWrapper(Path executeDirectory) {
    RenderingBackend backend = forgedAlliancePrefs.getRenderingBackend();
    if (!backend.requiresDownload()) {
      return Set.of();
    }

    if (!renderingWrapperService.ensureWrapperAvailable(backend)) {
      log.warn("Rendering wrapper for {} not available, falling back to DirectX 9", backend);
      return Set.of();
    }

    Set<Path> injectedFiles = new HashSet<>();
    Path wrapperDir = renderingWrapperService.getWrapperDirectory(backend);
    try {
      if (Files.isDirectory(wrapperDir)) {
        log.info("Injecting rendering wrapper from {} into {}", wrapperDir, executeDirectory);
        try (Stream<Path> stream = Files.list(wrapperDir)) {
          stream.filter(path -> path.toString().endsWith(".dll"))
              .forEach(sourceDll -> {
                Path targetDll = executeDirectory.resolve(sourceDll.getFileName());
                try {
                  Files.copy(sourceDll, targetDll, StandardCopyOption.REPLACE_EXISTING);
                  injectedFiles.add(targetDll);
                  log.debug("Injected wrapper DLL: {}", targetDll);
                } catch (IOException e) {
                  log.warn("Failed to inject wrapper DLL: {}", sourceDll.getFileName(), e);
                }
              });
        }
      }
    } catch (IOException e) {
      log.error("Failed to inject rendering wrapper", e);
    }
    return injectedFiles;
  }

  private void removeRenderingWrapper(Set<Path> injectedFiles) {
    for (Path targetDll : injectedFiles) {
      try {
        Files.deleteIfExists(targetDll);
        log.debug("Removed wrapper DLL: {}", targetDll);
      } catch (IOException e) {
        log.warn("Failed to remove wrapper DLL: {}", targetDll, e);
      }
    }
  }
}
