package com.faforever.client.vault.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.Controller;
import com.faforever.client.game.Game;
import com.faforever.client.i18n.I18n;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.util.Duration;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.Instant;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class WatchButtonController implements Controller<Node> {
  private final ReplayService replayService;
  private final ClientProperties clientProperties;
  private final TimeService timeService;

  public Button watchButton;
  private Game game;
  private final I18n i18n;
  private Timeline delayTimeline;

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

    watchButton.setDisable(true);
    watchButton.setOnAction(event -> replayService.runLiveReplay(game.getId()));
  }

  public void setGame(Game game) {
    this.game = game;
    Assert.notNull(game, "Game must not be null");
    Assert.notNull(game.getStartTime(), "The game's start must not be null: " + game);
    if (canWatch()) {
      allowWatch();
    } else {
      updateWatchButtonTimer();
      delayTimeline.play();
    }
  }

  private boolean canWatch() {
    java.time.Duration duration = getWatchDelayTime();
    return duration.isZero() || duration.isNegative();
  }

  private void allowWatch() {
    watchButton.setText(i18n.get("game.watch"));
    watchButton.setDisable(false);
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
    watchButton.setText(i18n.get("game.watchDelayedFormat", timeService.shortDuration(getWatchDelayTime())));
    watchButton.setDisable(true);
  }

  private java.time.Duration getWatchDelayTime() {
    Assert.notNull(game.getStartTime(),
        "Game's start time is null, in which case it shouldn't even be listed: " + game);
    return java.time.Duration.between(
        Instant.now(),
        game.getStartTime().plusSeconds(clientProperties.getReplay().getWatchDelaySeconds())
    );
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
