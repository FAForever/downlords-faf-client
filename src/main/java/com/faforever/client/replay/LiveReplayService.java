package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.DiscordSpectateEvent;
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
import javafx.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

import static com.faforever.client.util.Assert.checkNullIllegalState;
import static java.net.URLDecoder.decode;
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
  private final ObjectProperty<Pair<Integer, LiveReplayAction>> trackingReplayProperty = new SimpleObjectProperty<>(null);

  @Override
  public void afterPropertiesSet() throws Exception {
    JavaFxUtil.addListener(gameService.gameRunningProperty(), observable -> {
      if (gameService.isGameRunning()) {
        stopTrackingReplay();
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

  public Duration getWatchDelayTime(GameBean game) {
    Assert.notNull(game.getStartTime(), "Game's start time is null, in which case it shouldn't even be listed: " + game);
    return Duration.between(OffsetDateTime.now(), game.getStartTime().plusSeconds(getWatchDelaySeconds())
    );
  }

  public void performActionWhenAvailable(GameBean game, LiveReplayAction action) {
    checkNullIllegalState(game.getId(), "No game id to schedule future task");
    stopTrackingReplay();
    trackingReplayProperty.set(new Pair<>(game.getId(), action));
    switch (action) {
      case NOTIFY_ME -> notifyUserWhenReplayAvailable(game);
      case RUN_REPLAY -> runLiveReplayWhenAvailable(game);
      default -> throw new IllegalStateException("Unexpected value: " + action);
    }
  }

  private void notifyUserWhenReplayAvailable(GameBean game) {
    futureTask = taskScheduler.schedule(() -> {
      clearTrackingReplayProperty();
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
      clearTrackingReplayProperty();
      runLiveReplay(game.getId());
    }, Instant.from(game.getStartTime().plusSeconds(getWatchDelaySeconds())));
  }

  private void clearTrackingReplayProperty() {
    trackingReplayProperty.set(null);
  }

  public void stopTrackingReplay() {
    if (futureTask != null && !futureTask.isCancelled()) {
      futureTask.cancel(false);
      clearTrackingReplayProperty();
    }
    futureTask = null;
  }

  public ObjectProperty<Pair<Integer, LiveReplayAction>> getTrackingReplayProperty() {
    return trackingReplayProperty;
  }

  public Optional<Pair<Integer, LiveReplayAction>> getTrackingReplay() {
    return Optional.ofNullable(trackingReplayProperty.get());
  }

  public void runLiveReplay(int gameId) {
    GameBean game = gameService.getByUid(gameId);
    if (game == null) {
      throw new RuntimeException("There's no game with ID: " + gameId);
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
    log.debug("Running replay from URL: {}", uri);
    if (!uri.getScheme().equals(FAF_LIVE_PROTOCOL)) {
      throw new IllegalArgumentException("Invalid protocol: " + uri.getScheme());
    }

    Map<String, String> queryParams = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(uri.getQuery());

    try {
      String gameType = queryParams.get("mod");
      String mapName = decode(queryParams.get("map"), UTF_8.name());
      Integer gameId = Integer.parseInt(uri.getPath().split("/")[1]);
      URI replayUri = new URI(GPGNET_SCHEME, null, uri.getHost(), uri.getPort(), uri.getPath(), null, null);
      gameService.runWithLiveReplay(replayUri, gameId, gameType, mapName)
          .exceptionally(throwable -> {
            notificationService.addNotification(new ImmediateNotification(
                i18n.get("errorTitle"),
                i18n.get("liveReplayCouldNotBeStarted"),
                Severity.ERROR, throwable,
                List.of(new CopyErrorAction(i18n, reportingService, throwable), new GetHelpAction(i18n, reportingService), new DismissAction(i18n))
            ));
            return null;
          });
    } catch (URISyntaxException | UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @EventListener
  public void onDiscordGameJoinEvent(DiscordSpectateEvent discordSpectateEvent) {
    Integer replayId = discordSpectateEvent.getReplayId();
    runLiveReplay(replayId);
  }

  @Override
  public void destroy() throws Exception {
    stopTrackingReplay();
  }

  public enum LiveReplayAction {
    NOTIFY_ME, RUN_REPLAY
  }
}
