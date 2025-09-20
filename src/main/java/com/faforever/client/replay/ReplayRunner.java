package com.faforever.client.replay;

import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.exception.NotifiableException;
import com.faforever.client.fa.ForgedAllianceLaunchService;
import com.faforever.client.featuredmod.FeaturedModService;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.game.GamePathHandler;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Downloads necessary maps, mods and updates before starting
 */
@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class ReplayRunner implements InitializingBean {

  private static final String GPGNET_SCHEME = "gpgnet";

  private final ForgedAllianceLaunchService forgedAllianceLaunchService;
  private final MapService mapService;
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ModService modService;
  private final FeaturedModService featuredModService;
  private final PlayerService playerService;
  private final GamePathHandler gamePathHandler;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final LiveReplayProxyServer liveReplayProxyServer;

  private final ReadOnlyObjectWrapper<Process> process = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyBooleanWrapper running = new ReadOnlyBooleanWrapper();

  @Override
  public void afterPropertiesSet() {
    running.bind(process.flatMap(process -> {
      BooleanProperty isAlive = new SimpleBooleanProperty(process.isAlive());
      process.onExit().thenRunAsync(() -> isAlive.set(false), fxApplicationThreadExecutor);
      return isAlive;
    }));
  }

  @VisibleForTesting
  Mono<Void> downloadMapAskIfError(String mapFolderName) {
    return mapService.downloadIfNecessary(mapFolderName).onErrorResume(this::shouldStartWithOutMap);
  }

  /**
   * @param path a replay file that is readable by the preferences without any further conversion
   */
  public void runWithReplay(Path path, @Nullable Integer replayId, String featuredModName, Integer baseFafVersion,
                            Map<String, Integer> featuredModFileVersions, Set<String> simMods, String mapFolderName) {
    if (isRunning()) {
      log.info("Another replay is already running, not starting replay");
      notificationService.addImmediateWarnNotification("replay.replayRunning");
      return;
    }

    if (!preferencesService.hasValidGamePath()) {
      gamePathHandler.chooseAndValidateGameDirectory()
                     .thenAccept(pathSet -> runWithReplay(path, replayId, featuredModName, baseFafVersion,
                                                          featuredModFileVersions, simMods, mapFolderName));
      return;
    }

    CompletableFuture<Void> updateFeaturedModFuture = featuredModService.updateFeaturedMod(featuredModName,
                                                                                           featuredModFileVersions,
                                                                                           baseFafVersion, true);
    boolean hasSimMods = simMods == null || simMods.isEmpty();
    CompletableFuture<Void> installAndActivateSimModsFuture = hasSimMods ? completedFuture(
        null) : modService.downloadAndEnableMods(simMods).toFuture();
    CompletableFuture<Void> downloadMapFuture = downloadMapAskIfError(mapFolderName).toFuture();
    CompletableFuture.allOf(updateFeaturedModFuture, installAndActivateSimModsFuture, downloadMapFuture)
                     .thenApply(ignored -> forgedAllianceLaunchService.startReplay(path, replayId))
                     .thenAcceptAsync(process::set, fxApplicationThreadExecutor);
  }

  private Mono<Void> shouldStartWithOutMap(Throwable throwable) {
    return Mono.create(sink -> {
      List<Action> actions = List.of(new Action(i18n.get("replay.ignoreMapNotFound"), sink::success),
                                     new Action(i18n.get("replay.abortAfterMapNotFound"), () -> sink.error(throwable)));

      ImmediateNotification notification = new ImmediateNotification(i18n.get("replay.mapDownloadFailed"),
                                                                     i18n.get("replay.mapDownloadFailed.wannaContinue"),
                                                                     Severity.WARN, actions);
      notificationService.addNotification(notification);
    });
  }

  public void runWithLiveReplay(GameInfo game) {
    if (isRunning()) {
      log.info("Another replay is already running, not starting replay");
      notificationService.addImmediateWarnNotification("replay.replayRunning");
      return;
    }

    if (!preferencesService.hasValidGamePath()) {
      gamePathHandler.chooseAndValidateGameDirectory().thenRun(() -> runWithLiveReplay(game));
      return;
    }

    int port = liveReplayProxyServer.start();

    /* A courtesy towards the replay server so we can see in logs who we're dealing with. */
    String playerName = playerService.getCurrentPlayer().getUsername();
    String featuredModName = game.getFeaturedMod();
    String mapName = game.getMapFolderName();
    URI replayUrl = UriComponentsBuilder.newInstance()
                                        .scheme(GPGNET_SCHEME)
                                        .host(InetAddress.getLoopbackAddress().getHostAddress())
                                        .port(port)
                                        .pathSegment(String.valueOf(game.getId()),
                                                     playerName + ReplayService.SUP_COM_REPLAY_FILE_ENDING)
                                        .build()
                                        .toUri();

    Set<String> simModUids = game.getSimMods().keySet();

    CompletableFuture<Void> updateFeaturedModFuture = featuredModService.updateFeaturedModToLatest(featuredModName,
                                                                                                   true);
    CompletableFuture<Void> installAndActivateSimModsFuture = simModUids.isEmpty() ? completedFuture(
        null) : modService.downloadAndEnableMods(simModUids).toFuture();
    CompletableFuture<Void> downloadMapFuture = downloadMapAskIfError(mapName).toFuture();
    CompletableFuture.allOf(updateFeaturedModFuture, installAndActivateSimModsFuture, downloadMapFuture)
                     .thenApply(ignored -> forgedAllianceLaunchService.startReplay(replayUrl, game.getId()))
                     .thenApplyAsync(process -> {
                       this.process.set(process);
                       return process;
                     }, fxApplicationThreadExecutor)
                     .exceptionally(throwable -> {
                       if (throwable instanceof NotifiableException notifiableException) {
                         notificationService.addErrorNotification(notifiableException);
                       } else {
                         notificationService.addImmediateErrorNotification(throwable, "liveReplayCouldNotBeStarted");
                       }
                       return null;
                     })
                     .thenCompose(Process::onExit)
                     .whenComplete((ignored, throwable) -> liveReplayProxyServer.stop());
  }

  public boolean isRunning() {
    return running.get();
  }

  public void killReplay() {
    if (isRunning()) {
      log.info("Forged Alliance replay still running, destroying process");
      process.get().destroy();
    }
  }
}
