package com.faforever.client.vault.replay;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleInvalidationListener;
import com.faforever.client.fx.contextmenu.CancelActionNotifyMeMenuItem;
import com.faforever.client.fx.contextmenu.CancelActionRunReplayImmediatelyMenuItem;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.NotifyMeMenuItem;
import com.faforever.client.fx.contextmenu.RunReplayImmediatelyMenuItem;
import com.faforever.client.i18n.I18n;
import com.faforever.client.replay.LiveReplayService;
import com.faforever.client.replay.TrackingLiveReplay;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class WatchButtonController implements Controller<Node> {

  public static final PseudoClass TRACKABLE_PSEUDO_CLASS = PseudoClass.getPseudoClass("trackable");

  private final LiveReplayService liveReplayService;
  private final TimeService timeService;
  private final I18n i18n;
  private final ContextMenuBuilder contextMenuBuilder;

  private final ObjectProperty<GameBean> game = new SimpleObjectProperty<>();

  public Button watchButton;

  private Timeline delayTimeline;
  private ContextMenu contextMenu;

  private final SimpleInvalidationListener trackingLiveReplayPropertyListener = this::updateWatchButtonState;


  public void initialize() {
    delayTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> onFinished()));
    delayTimeline.setCycleCount(Timeline.INDEFINITE);
    JavaFxUtil.addListener(liveReplayService.getTrackingLiveReplayProperty(), new WeakInvalidationListener(trackingLiveReplayPropertyListener));
  }

  public void setGame(GameBean game) {
    Assert.notNull(game, "Game must not be null");
    Assert.notNull(game.getStartTime(), "The game's start must not be null: " + game);

    this.game.set(game);
    if (liveReplayService.canWatchReplay(game)) {
      allowWatch();
    } else {
      updateWatchButtonTimer();
      updateWatchButtonState();
      delayTimeline.play();
      watchButton.setOnAction(event -> showContextMenu());
    }
  }

  public GameBean getGame() {
    return game.get();
  }

  public ObjectProperty<GameBean> gameProperty() {
    return game;
  }

  private void showContextMenu() {
    Bounds screenBounds = watchButton.localToScreen(watchButton.getBoundsInLocal());
    GameBean gameBean = getGame();
    contextMenu = contextMenuBuilder.newBuilder()
        .addItem(NotifyMeMenuItem.class, gameBean)
        .addItem(CancelActionNotifyMeMenuItem.class, gameBean)
        .addItem(RunReplayImmediatelyMenuItem.class, gameBean)
        .addItem(CancelActionRunReplayImmediatelyMenuItem.class, gameBean)
        .build();
    contextMenu.show(watchButton.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY());
  }

  private void allowWatch() {
    JavaFxUtil.runLater(() -> {
      watchButton.setOnAction(event -> liveReplayService.runLiveReplay(getGame().getId()));
      watchButton.setText(i18n.get("game.watch"));
    });
  }

  private void onFinished() {
    if (liveReplayService.canWatchReplay(getGame())) {
      delayTimeline.stop();
      allowWatch();
      if (contextMenu != null && contextMenu.isShowing()) {
        contextMenu.hide();
      }
    } else {
      updateWatchButtonTimer();
    }
  }

  private void updateWatchButtonState() {
    liveReplayService.getTrackingLiveReplay().map(TrackingLiveReplay::getGameId).ifPresentOrElse(gameId -> {
      GameBean gameBean = getGame();
      boolean isTracking = gameBean != null && gameBean.getId().equals(gameId);
      JavaFxUtil.runLater(() -> watchButton.pseudoClassStateChanged(TRACKABLE_PSEUDO_CLASS, isTracking));
    }, () -> JavaFxUtil.runLater(() -> watchButton.pseudoClassStateChanged(TRACKABLE_PSEUDO_CLASS, false)));
  }

  private void updateWatchButtonTimer() {
    JavaFxUtil.runLater(() -> watchButton.setText(i18n.get("game.watchDelayedFormat",
        timeService.shortDuration(liveReplayService.getWatchDelayTime(getGame())))));
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
