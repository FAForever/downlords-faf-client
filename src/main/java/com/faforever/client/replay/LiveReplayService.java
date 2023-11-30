package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.CopyErrorAction;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.GetHelpAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.reporting.ReportingService;
import com.google.common.base.Splitter;
import com.google.common.net.UrlEscapers;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

import static com.faforever.client.util.Assert.checkNullIllegalState;
import static java.nio.charset.StandardCharsets.UTF_8;

@Lazy
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveReplayService implements InitializingBean, DisposableBean {

  private static final String FAF_LIVE_PROTOCOL = "faflive";
  private static final String GPGNET_SCHEME = "gpgnet";

  private final ClientProperties clientProperties;
  private final TaskScheduler taskScheduler;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final GameService gameService;
  private final PlayerService playerService;
  private final ReportingService reportingService;

  private Future<?> futureTask;
  private final ObjectProperty<TrackingLiveReplay> trackingLiveReplayProperty = new SimpleObjectProperty<>(null);

  @Override
  public void afterPropertiesSet() throws Exception {
    JavaFxUtil.addListener(gameService.gameRunningProperty(), observable -> {
      if (gameService.isGameRunning()) {
        stopTrackingLiveReplay();
      }
    });
  }

  private Integer getWatchDelaySeconds() {
    return clientProperties.getReplay().getWatchDelaySeconds();
  }

  public boolean canWatchReplay(GameBean game) {
    Duration duration = getWatchDelayTime(game);
    return duration.isZero() || duration.isNegative();
  }

  public boolean canWatchReplay(OffsetDateTime startTime) {
    Duration duration = Duration.between(OffsetDateTime.now(), startTime.plusSeconds(getWatchDelaySeconds()));
    return duration.isZero() || duration.isNegative();
  }

  public Duration getWatchDelayTime(GameBean game) {
    Assert.notNull(game.getStartTime(), "Game's start time is null, in which case it shouldn't even be listed: " + game);
    return Duration.between(OffsetDateTime.now(), game.getStartTime().plusSeconds(getWatchDelaySeconds()));
  }

  public void performActionWhenAvailable(GameBean game, TrackingLiveReplayAction action) {
    checkNullIllegalState(game.getId(), "No game id to schedule future task");
    stopTrackingLiveReplay();
    trackingLiveReplayProperty.set(new TrackingLiveReplay(game.getId(), action));
    switch (action) {
      case NOTIFY_ME -> notifyUserWhenReplayAvailable(game);
      case RUN_REPLAY -> runLiveReplayWhenAvailable(game);
      default -> throw new IllegalStateException("Unexpected value: " + action);
    }
  }

  private void notifyUserWhenReplayAvailable(GameBean game) {
    futureTask = taskScheduler.schedule(() -> {
      clearTrackingLiveReplayProperty();
      notificationService.addNotification(new PersistentNotification(
          i18n.get("vault.liveReplays.replayAvailable", game.getTitle()),
          Severity.INFO,
          List.of(new Action(i18n.get("game.watch"), (event) -> runLiveReplay(game.getId())))));
    }, Instant.from(game.getStartTime().plusSeconds(getWatchDelaySeconds())));
  }

  private void runLiveReplayWhenAvailable(GameBean game) {
    futureTask = taskScheduler.schedule(() -> {
      notificationService.addNotification(new TransientNotification(
          i18n.get("vault.liveReplays.replayAvailable", game.getTitle()),
          i18n.get("vault.liveReplays.replayLaunching")));
      clearTrackingLiveReplayProperty();
      runLiveReplay(game.getId());
    }, Instant.from(game.getStartTime().plusSeconds(getWatchDelaySeconds())));
  }

  private void clearTrackingLiveReplayProperty() {
    trackingLiveReplayProperty.set(null);
  }

  public void stopTrackingLiveReplay() {
    if (futureTask != null && !futureTask.isCancelled()) {
      futureTask.cancel(false);
      clearTrackingLiveReplayProperty();
    }
    futureTask = null;
  }

  public ObjectProperty<TrackingLiveReplay> trackingLiveReplayProperty() {
    return trackingLiveReplayProperty;
  }

  public Optional<TrackingLiveReplay> getTrackingLiveReplay() {
    return Optional.ofNullable(trackingLiveReplayProperty.get());
  }

  public void runLiveReplay(int gameId) {
    GameBean game = gameService.getByUid(gameId);
    if (game == null) {
      log.warn("No game with ID `{}`", gameId);
      return;
    }
    /* A courtesy towards the replay server so we can see in logs who we're dealing with. */
    String playerName = playerService.getCurrentPlayer().getUsername();

    URI uri = UriComponentsBuilder.newInstance()
        .scheme(FAF_LIVE_PROTOCOL)
        .host(clientProperties.getReplay().getRemoteHost())
        .path("/" + gameId + "/" + playerName + ReplayService.SUP_COM_REPLAY_FILE_ENDING)
        .queryParam("map", UrlEscapers.urlFragmentEscaper().escape(game.getMapFolderName()))
        .queryParam("mod", game.getFeaturedMod())
        .build()
        .toUri();

    runLiveReplay(uri);
  }


  public void runLiveReplay(URI uri) {
    log.info("Running replay from URL: `{}`", uri);
    if (!uri.getScheme().equals(FAF_LIVE_PROTOCOL)) {
      throw new IllegalArgumentException("Invalid protocol: " + uri.getScheme());
    }

    Map<String, String> queryParams = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(uri.getQuery());

    String gameType = queryParams.get("mod");
    String mapName = URLDecoder.decode(queryParams.get("map"), UTF_8);
    Integer gameId = Integer.parseInt(uri.getPath().split("/")[1]);
    URI replayUri = UriComponentsBuilder.newInstance()
                                        .scheme(GPGNET_SCHEME)
                                        .host(uri.getHost())
                                        .port(uri.getPort())
                                        .path(uri.getPath())
                                        .build()
                                        .toUri();
    gameService.runWithLiveReplay(replayUri, gameId, gameType, mapName)
               .exceptionally(throwable -> {
                 notificationService.addNotification(new ImmediateNotification(
                     i18n.get("errorTitle"),
                     i18n.get("liveReplayCouldNotBeStarted"),
                     Severity.ERROR, throwable,
                     List.of(new CopyErrorAction(i18n, reportingService, throwable),
                             new GetHelpAction(i18n, reportingService), new DismissAction(i18n))
                 ));
                 return null;
               });
  }

  @Override
  public void destroy() throws Exception {
    stopTrackingLiveReplay();
  }
}
