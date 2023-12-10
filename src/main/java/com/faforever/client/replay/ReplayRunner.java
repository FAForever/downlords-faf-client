package com.faforever.client.replay;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fa.ForgedAllianceLaunchService;
import com.faforever.client.featuredmod.FeaturedModService;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.GameService;
import com.faforever.client.game.error.GameLaunchException;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.ui.preferences.GameDirectoryRequiredHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Downloads necessary maps, mods and updates before starting
 */
@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class ReplayRunner {

  private final ForgedAllianceLaunchService forgedAllianceLaunchService;
  private final MapService mapService;
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ModService modService;
  private final FeaturedModService featuredModService;
  private final GameService gameService;
  private final GameDirectoryRequiredHandler gameDirectoryRequiredHandler;

  private Process process;

  private CompletableFuture<Void> downloadMapIfNecessary(String mapFolderName) {
    return mapService.downloadIfNecessary(mapFolderName).handle((ignored, throwable) -> {
      if (throwable == null) {
        return null;
      }

      try {
        askWhetherToStartWithOutMap(throwable);
      } catch (Throwable e) {
        throw new CompletionException(e);
      }

      return null;
    });
  }

  /**
   * @param path a replay file that is readable by the preferences without any further conversion
   */
  public CompletableFuture<Void> runWithReplay(Path path, @Nullable Integer replayId, String featuredModName,
                                               Integer baseFafVersion, Map<String, Integer> featuredModFileVersions,
                                               Set<String> simMods, String mapFolderName) {
    if (!canStartReplay()) {
      return completedFuture(null);
    }

    if (!preferencesService.isValidGamePath()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      gameDirectoryFuture.thenAccept(
          pathSet -> runWithReplay(path, replayId, featuredModName, baseFafVersion, featuredModFileVersions, simMods,
                                   mapFolderName));
      return completedFuture(null);
    }

    CompletableFuture<Void> updateFeaturedModFuture = featuredModService.updateFeaturedMod(featuredModName,
                                                                                           featuredModFileVersions,
                                                                                           baseFafVersion, true);
    CompletableFuture<Void> installAndActivateSimModsFuture = modService.installAndEnableMods(simMods);
    CompletableFuture<Void> downloadMapFuture = downloadMapIfNecessary(mapFolderName);
    return CompletableFuture.allOf(updateFeaturedModFuture, installAndActivateSimModsFuture, downloadMapFuture)
                            .thenRun(() -> {
                              try {
                                this.process = forgedAllianceLaunchService.startReplay(path, replayId);
                              } catch (IOException e) {
                                throw new CompletionException(e);
                              }
                            });
  }

  private boolean canStartReplay() {
    if (process != null && process.isAlive()) {
      log.info("Another replay is already running, not starting replay");
      notificationService.addImmediateWarnNotification("replay.replayRunning");
      return false;
    }
    return true;
  }

  public CompletableFuture<Path> postGameDirectoryChooseEvent() {
    CompletableFuture<Path> gameDirectoryFuture = new CompletableFuture<>();
    gameDirectoryRequiredHandler.onChooseGameDirectory();
    return gameDirectoryFuture;
  }

  private void askWhetherToStartWithOutMap(Throwable throwable) throws Throwable {
    JavaFxUtil.assertBackgroundThread();
    log.error("Error loading map for replay", throwable);

    CountDownLatch userAnswered = new CountDownLatch(1);
    AtomicReference<Boolean> proceed = new AtomicReference<>(false);
    List<Action> actions = Arrays.asList(new Action(i18n.get("replay.ignoreMapNotFound"), event -> {
      proceed.set(true);
      userAnswered.countDown();
    }), new Action(i18n.get("replay.abortAfterMapNotFound"), event -> userAnswered.countDown()));
    notificationService.addNotification(new ImmediateNotification(i18n.get("replay.mapDownloadFailed"),
                                                                  i18n.get("replay.mapDownloadFailed.wannaContinue"),
                                                                  Severity.WARN, actions));
    userAnswered.await();
    if (!proceed.get()) {
      throw throwable;
    }
  }

  public CompletableFuture<Void> runWithLiveReplay(URI replayUrl, Integer gameId, String gameType, String mapName) {
    if (!canStartReplay()) {
      return completedFuture(null);
    }

    if (!preferencesService.isValidGamePath()) {
      CompletableFuture<Path> gameDirectoryFuture = postGameDirectoryChooseEvent();
      return gameDirectoryFuture.thenCompose(path -> runWithLiveReplay(replayUrl, gameId, gameType, mapName));
    }

    Set<String> simModUids = gameService.getByUid(gameId).map(GameBean::getSimMods).map(Map::keySet).orElse(Set.of());

    CompletableFuture<Void> updateFeaturedModFuture = featuredModService.updateFeaturedModToLatest(gameType, true);
    CompletableFuture<Void> installAndActivateSimModsFuture = modService.installAndEnableMods(simModUids);
    CompletableFuture<Void> downloadMapFuture = downloadMapIfNecessary(mapName);
    return CompletableFuture.allOf(updateFeaturedModFuture, installAndActivateSimModsFuture, downloadMapFuture)
                            .thenRun(() -> {
                              try {
                                this.process = forgedAllianceLaunchService.startReplay(replayUrl, gameId);
                              } catch (IOException e) {
                                throw new GameLaunchException("Live replay could not be started", e,
                                                              "replay.live.startError");
                              }
                            });
  }

  private boolean isRunning() {
    return process != null && process.isAlive();
  }

  public void killReplay() {
    if (isRunning()) {
      log.info("Forged Alliance replay still running, destroying process");
      process.destroy();
    }
  }
}
