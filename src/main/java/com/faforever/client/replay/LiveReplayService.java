package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Pair;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.concurrent.Future;

import static com.faforever.client.util.Assert.checkNullIllegalState;

@Lazy
@Service
@RequiredArgsConstructor
public class LiveReplayService implements InitializingBean, DisposableBean {

  private final ClientProperties clientProperties;
  private final TaskScheduler taskScheduler;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ReplayService replayService;
  private final GameService gameService;

  private Integer watchDelaySeconds;
  private Future<?> futureTask;
  private final ObjectProperty<Pair<Integer, LiveReplayAction>> futureReplayProperty = new SimpleObjectProperty<>(null);

  @Override
  public void afterPropertiesSet() throws Exception {
    watchDelaySeconds = clientProperties.getReplay().getWatchDelaySeconds();
    JavaFxUtil.addListener(gameService.gameRunningProperty(), (observable, oldValue, newValue) -> {
      if (newValue.equals(true) && futureReplayProperty.getValue() != null) {
        cancelScheduledTask();
      }
    });
  }

  public boolean canWatch(GameBean game) {
    Duration duration = getWatchDelayTime(game);
    return duration.isZero() || duration.isNegative();
  }

  public Duration getWatchDelayTime(GameBean game) {
    Assert.notNull(game.getStartTime(), "Game's start time is null, in which case it shouldn't even be listed: " + game);
    return Duration.between(OffsetDateTime.now(), game.getStartTime().plusSeconds(clientProperties.getReplay().getWatchDelaySeconds())
    );
  }

  public void performActionWhenAvailable(GameBean game, LiveReplayAction action) {
    checkNullIllegalState(game.getId(), "No game id to schedule future task");
    cancelScheduledTask();
    futureReplayProperty.set(new Pair<>(game.getId(), action));
    switch (action) {
      case NOTIFY_ME -> notifyUserWhenReplayIsAvailable(game);
      case RUN -> runLiveReplayWhenIsAvailable(game);
    }
  }

  private void notifyUserWhenReplayIsAvailable(GameBean game) {
    futureTask = taskScheduler.schedule(() -> {
      clearFutureReplayProperty();
      notificationService.addNotification(new TransientNotification(
          i18n.get("vault.liveReplays.notifyMe.replayAvailable", game.getTitle()),
          i18n.get("vault.liveReplays.notifyMe.replayAvailable.click"),
          null,
          (event) -> replayService.runLiveReplay(game.getId())));
    }, Instant.from(game.getStartTime().plusSeconds(watchDelaySeconds)));
  }

  private void runLiveReplayWhenIsAvailable(GameBean game) {
    futureTask = taskScheduler.schedule(() -> {
      notificationService.addNotification(new TransientNotification(
          i18n.get("vault.liveReplays.notifyMe.replayAvailable", game.getTitle()),
          i18n.get("vault.liveReplays.scheduledRunReplay", 10)));

      futureTask = taskScheduler.schedule(() -> {
        clearFutureReplayProperty();
        replayService.runLiveReplay(game.getId());
      }, Instant.from(OffsetDateTime.now().plusSeconds(10)));

    }, Instant.from(game.getStartTime().plusSeconds(watchDelaySeconds).minusSeconds(10)));
  }

  private void clearFutureReplayProperty() {
    futureReplayProperty.set(null);
  }

  public void cancelScheduledTask() {
    if (futureTask != null && !futureTask.isCancelled()) {
      futureTask.cancel(false);
      clearFutureReplayProperty();
    }
    futureTask = null;
  }

  public ObjectProperty<Pair<Integer, LiveReplayAction>> getFutureReplayProperty() {
    return futureReplayProperty;
  }

  @Override
  public void destroy() throws Exception {
    cancelScheduledTask();
  }

  public enum LiveReplayAction {
    NOTIFY_ME, RUN
  }
}
