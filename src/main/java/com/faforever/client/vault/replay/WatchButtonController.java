package com.faforever.client.vault.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class WatchButtonController implements Controller<Node> {
  private final ReplayService replayService;
  private final ClientProperties clientProperties;
  private final TimeService timeService;

  public WatchLiveReplaySplitMenuButton watchButton;
  public MenuItem notifyMeItem;
  public MenuItem runReplayItem;
  private GameBean game;
  private final I18n i18n;
  private Timeline delayTimeline;

  private ChangeListener<Number> gameIdForNotifyMeListener;
  private ChangeListener<Number> gameIdForScheduleRunReplayListener;

  public WatchButtonController(ReplayService replayService, ClientProperties clientProperties, TimeService timeService, I18n i18n) {
    this.replayService = replayService;
    this.clientProperties = clientProperties;
    this.timeService = timeService;
    this.i18n = i18n;
  }

  public void initialize() {
    delayTimeline = new Timeline(
        new KeyFrame(Duration.ZERO, event -> onFinished()),
        new KeyFrame(Duration.seconds(1))
    );
    delayTimeline.setCycleCount(Timeline.INDEFINITE);
    watchButton.setIsUnavailableSupplier(() -> !canWatch());
  }

  public void setGame(GameBean game) {
    Assert.notNull(game, "Game must not be null");
    Assert.notNull(game.getStartTime(), "The game's start must not be null: " + game);

    this.game = game;
    if (canWatch()) {
      allowWatch();
    } else {
      initializeListeners();
      updateWatchButtonTimer();
      delayTimeline.play();
    }
  }

  private void initializeListeners() {
    gameIdForNotifyMeListener = (observable, oldValue, newValue) ->
        JavaFxUtil.runLater(() -> {
          if (newValue.equals(game.getId())) {
            notifyMeItem.setText(i18n.get("vault.liveReplays.contextMenu.notifyMe.cancel"));
            notifyMeItem.setOnAction(event -> replayService.cancelNotifyMeWhenReplayAvailableIn());
          } else {
            notifyMeItem.setText(i18n.get("vault.liveReplays.contextMenu.notifyMe"));
            notifyMeItem.setOnAction(event -> notifyMeWhenReplayAvailable());
          }
        });
    gameIdForScheduleRunReplayListener = (observable, oldValue, newValue) ->
        JavaFxUtil.runLater(() -> {
          if (newValue.equals(game.getId())) {
            runReplayItem.setText(i18n.get("vault.liveReplays.contextMenu.runImmediately.cancel"));
            runReplayItem.setOnAction(event -> replayService.cancelScheduleRunReplayIn());
          } else {
            runReplayItem.setText(i18n.get("vault.liveReplays.contextMenu.runImmediately"));
            runReplayItem.setOnAction(event -> runWhenReplayAvailable());
          }
        });

    JavaFxUtil.addListener(replayService.getGameIdForNotifyMeProperty(), gameIdForNotifyMeListener);
    JavaFxUtil.addListener(replayService.getGameIdForScheduleRunReplayProperty(), gameIdForScheduleRunReplayListener);
  }

  private boolean canWatch() {
    java.time.Duration duration = getWatchDelayTime();
    return duration.isZero() || duration.isNegative();
  }

  private void allowWatch() {
    JavaFxUtil.runLater(() -> {
      watchButton.setOnAction(event -> replayService.runLiveReplay(game.getId()));
      watchButton.setText(i18n.get("game.watch"));
      watchButton.pseudoClassStateChanged(LiveReplayController.AVAILABLE_PSEUDO_CLASS, true);
    });
    removeListeners();
  }

  private void removeListeners() {
    if (gameIdForNotifyMeListener != null) {
      JavaFxUtil.removeListener(replayService.getGameIdForNotifyMeProperty(), gameIdForNotifyMeListener);
    }
    if (gameIdForScheduleRunReplayListener != null) {
      JavaFxUtil.removeListener(replayService.getGameIdForScheduleRunReplayProperty(), gameIdForScheduleRunReplayListener);
    }
  }

  private void onFinished() {
    if (canWatch()) {
      delayTimeline.stop();
      allowWatch();
    } else {
      updateWatchButtonTimer();
    }
  }

  private void updateWatchButtonTimer() {
    JavaFxUtil.runLater(() -> watchButton.setText(i18n.get("game.watchDelayedFormat", timeService.shortDuration(getWatchDelayTime()))));
  }

  private java.time.Duration getWatchDelayTime() {
    Assert.notNull(game.getStartTime(),
        "Game's start time is null, in which case it shouldn't even be listed: " + game);
    return java.time.Duration.between(
        OffsetDateTime.now(),
        game.getStartTime().plusSeconds(clientProperties.getReplay().getWatchDelaySeconds())
    );
  }

  public void notifyMeWhenReplayAvailable() {
    replayService.notifyMeWhenReplayAvailableIn(getWatchDelayTime(), game);
  }

  public void runWhenReplayAvailable() {
    replayService.scheduleRunReplayIn(getWatchDelayTime(), game);
  }

  @VisibleForTesting
  public Timeline getDelayTimeline() {
    return delayTimeline;
  }

  @Override
  public Node getRoot() {
    return watchButton;
  }
}
