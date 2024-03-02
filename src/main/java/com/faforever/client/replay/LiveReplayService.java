package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.game.GameRunner;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotification;
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

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import static com.faforever.client.util.Assert.checkNullIllegalState;

@Lazy
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveReplayService implements InitializingBean, DisposableBean {

  private final ClientProperties clientProperties;
  private final TaskScheduler taskScheduler;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final GameRunner gameRunner;
  private final ReplayRunner replayRunner;

  private final ObjectProperty<TrackingLiveReplay> trackingLiveReplayProperty = new SimpleObjectProperty<>(null);

  private Future<?> replayAvailableTask;

  @Override
  public void afterPropertiesSet() throws Exception {
    gameRunner.runningProperty().subscribe(gameRunning -> {
      if (gameRunning) {
        stopTrackingLiveReplay();
      }
    });
  }

  private Integer getWatchDelaySeconds() {
    return clientProperties.getReplay().getWatchDelaySeconds();
  }

  public boolean canWatchReplay(GameInfo game) {
    Duration duration = getWatchDelayTime(game);
    return duration.isZero() || duration.isNegative();
  }

  public boolean canWatchReplay(OffsetDateTime startTime) {
    Duration duration = Duration.between(OffsetDateTime.now(), startTime.plusSeconds(getWatchDelaySeconds()));
    return duration.isZero() || duration.isNegative();
  }

  public Duration getWatchDelayTime(GameInfo game) {
    Assert.notNull(game.getStartTime(), "Game's start time is null, in which case it shouldn't even be listed: " + game);
    return Duration.between(OffsetDateTime.now(), game.getStartTime().plusSeconds(getWatchDelaySeconds()));
  }

  public void performActionWhenAvailable(GameInfo game, TrackingLiveReplayAction action) {
    checkNullIllegalState(game.getId(), "No game id to schedule future task");
    stopTrackingLiveReplay();
    trackingLiveReplayProperty.set(new TrackingLiveReplay(game.getId(), action));
    switch (action) {
      case NOTIFY_ME -> notifyUserWhenReplayAvailable(game);
      case RUN_REPLAY -> runLiveReplayWhenAvailable(game);
      default -> throw new IllegalStateException("Unexpected value: " + action);
    }
  }

  private void notifyUserWhenReplayAvailable(GameInfo game) {
    replayAvailableTask = taskScheduler.schedule(() -> {
      clearTrackingLiveReplayProperty();
      notificationService.addNotification(new PersistentNotification(
          i18n.get("vault.liveReplays.replayAvailable", game.getTitle()),
          Severity.INFO, List.of(new Action(i18n.get("game.watch"), () -> replayRunner.runWithLiveReplay(game)))));
    }, Instant.from(game.getStartTime().plusSeconds(getWatchDelaySeconds())));
  }

  private void runLiveReplayWhenAvailable(GameInfo game) {
    replayAvailableTask = taskScheduler.schedule(() -> {
      notificationService.addNotification(new TransientNotification(
          i18n.get("vault.liveReplays.replayAvailable", game.getTitle()),
          i18n.get("vault.liveReplays.replayLaunching")));
      clearTrackingLiveReplayProperty();
      replayRunner.runWithLiveReplay(game);
    }, Instant.from(game.getStartTime().plusSeconds(getWatchDelaySeconds())));
  }

  private void clearTrackingLiveReplayProperty() {
    trackingLiveReplayProperty.set(null);
  }

  public void stopTrackingLiveReplay() {
    if (replayAvailableTask != null && !replayAvailableTask.isCancelled()) {
      replayAvailableTask.cancel(false);
      clearTrackingLiveReplayProperty();
    }
    replayAvailableTask = null;
  }

  public ObjectProperty<TrackingLiveReplay> trackingLiveReplayProperty() {
    return trackingLiveReplayProperty;
  }

  public Optional<TrackingLiveReplay> getTrackingLiveReplay() {
    return Optional.ofNullable(trackingLiveReplayProperty.get());
  }

  @Override
  public void destroy() throws Exception {
    stopTrackingLiveReplay();
  }
}
