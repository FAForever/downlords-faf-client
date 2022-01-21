package com.faforever.client.vault.replay;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.replay.LiveReplayService;
import com.faforever.client.replay.LiveReplayService.LiveReplayAction;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.util.Duration;
import javafx.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import static com.faforever.client.replay.LiveReplayService.LiveReplayAction.NOTIFY_ME;
import static com.faforever.client.replay.LiveReplayService.LiveReplayAction.RUN;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class WatchButtonController implements Controller<Node> {

  public static final PseudoClass AVAILABLE_PSEUDO_CLASS = PseudoClass.getPseudoClass("available");
  public static final PseudoClass TRACKABLE_PSEUDO_CLASS = PseudoClass.getPseudoClass("trackable");

  private final LiveReplayService liveReplayService;
  private final ReplayService replayService;
  private final TimeService timeService;

  public WatchLiveReplaySplitMenuButton watchButton;
  public MenuItem notifyMeItem;
  public MenuItem runReplayItem;
  private GameBean game;
  private final I18n i18n;
  private Timeline delayTimeline;

  private ChangeListener<Pair<Integer, LiveReplayAction>> trackingReplayPropertyListener;

  public void initialize() {
    delayTimeline = new Timeline(
        new KeyFrame(Duration.ZERO, event -> onFinished()),
        new KeyFrame(Duration.seconds(1))
    );
    delayTimeline.setCycleCount(Timeline.INDEFINITE);
    watchButton.setIsCannotWatchSupplier(() -> !liveReplayService.canWatch(game));
    initializeMenuItemsByDefault();
  }

  public void setGame(GameBean game) {
    Assert.notNull(game, "Game must not be null");
    Assert.notNull(game.getStartTime(), "The game's start must not be null: " + game);

    this.game = game;
    if (liveReplayService.canWatch(game)) {
      allowWatch();
    } else {
      initializeListeners();
      updateWatchButtonTimer();
      delayTimeline.play();
    }
  }

  private void initializeListeners() {
    trackingReplayPropertyListener = (observable, oldValue, newValue) ->
        JavaFxUtil.runLater(() -> {
          if (newValue != null) {
            Integer ID = newValue.getKey();
            LiveReplayAction action = newValue.getValue();
            if (game.getId().equals(ID)) {
              switch (action) {
                case NOTIFY_ME -> {
                  notifyMeItem.setText(i18n.get("vault.liveReplays.contextMenu.notifyMe.cancel"));
                  notifyMeItem.setOnAction(event -> liveReplayService.stopTrackingReplay());
                }
                case RUN -> {
                  runReplayItem.setText(i18n.get("vault.liveReplays.contextMenu.runImmediately.cancel"));
                  runReplayItem.setOnAction(event -> liveReplayService.stopTrackingReplay());
                }
              }

              watchButton.pseudoClassStateChanged(TRACKABLE_PSEUDO_CLASS, true);
              return;
            }
          }

          initializeMenuItemsByDefault();
          watchButton.pseudoClassStateChanged(TRACKABLE_PSEUDO_CLASS, false);
        });
    JavaFxUtil.addAndTriggerListener(liveReplayService.getTrackingReplayProperty(), trackingReplayPropertyListener);
  }

  private void initializeMenuItemsByDefault() {
    notifyMeItem.setText(i18n.get("vault.liveReplays.contextMenu.notifyMe"));
    notifyMeItem.setOnAction(event -> liveReplayService.performActionWhenAvailable(game, NOTIFY_ME));

    runReplayItem.setText(i18n.get("vault.liveReplays.contextMenu.runImmediately"));
    runReplayItem.setOnAction(event -> liveReplayService.performActionWhenAvailable(game, RUN));
  }

  private void allowWatch() {
    JavaFxUtil.runLater(() -> {
      watchButton.setOnAction(event -> replayService.runLiveReplay(game.getId()));
      watchButton.setText(i18n.get("game.watch"));
      watchButton.pseudoClassStateChanged(AVAILABLE_PSEUDO_CLASS, true);
    });
    removeListeners();
  }

  private void removeListeners() {
    if (trackingReplayPropertyListener != null) {
      JavaFxUtil.removeListener(liveReplayService.getTrackingReplayProperty(), trackingReplayPropertyListener);
    }
  }

  private void onFinished() {
    if (liveReplayService.canWatch(game)) {
      delayTimeline.stop();
      allowWatch();
    } else {
      updateWatchButtonTimer();
    }
  }

  private void updateWatchButtonTimer() {
    JavaFxUtil.runLater(() -> watchButton.setText(i18n.get("game.watchDelayedFormat",
        timeService.shortDuration(liveReplayService.getWatchDelayTime(game)))));
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
