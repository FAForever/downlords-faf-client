package com.faforever.client.replay;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.contextmenu.CancelActionNotifyMeMenuItem;
import com.faforever.client.fx.contextmenu.CancelActionRunReplayImmediatelyMenuItem;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.NotifyMeMenuItem;
import com.faforever.client.fx.contextmenu.RunReplayImmediatelyMenuItem;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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

import java.time.OffsetDateTime;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class WatchButtonController extends NodeController<Node> {

  public static final PseudoClass TRACKABLE_PSEUDO_CLASS = PseudoClass.getPseudoClass("trackable");

  private final LiveReplayService liveReplayService;
  private final TimeService timeService;
  private final I18n i18n;
  private final ContextMenuBuilder contextMenuBuilder;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final ObjectProperty<GameBean> game = new SimpleObjectProperty<>();
  private final Timeline watchTimeTimeline = new Timeline(new KeyFrame(Duration.ZERO, event -> updateDisplay()), new KeyFrame(Duration.seconds(1)));

  public Button watchButton;

  private ContextMenu contextMenu;

  @Override
  protected void onInitialize() {
    watchTimeTimeline.setCycleCount(Timeline.INDEFINITE);

    game.flatMap(GameBean::startTimeProperty)
        .when(showing)
        .subscribe(this::checkGameTimeline);

    liveReplayService.trackingLiveReplayProperty()
        .map(TrackingLiveReplay::gameId)
                     .flatMap(trackedId -> game.flatMap(GameBean::idProperty).map(trackedId::equals))
                     .orElse(false)
        .when(showing)
                     .subscribe(this::updateButtonTrackingClass);

    watchButton.onActionProperty()
        .bind(game.flatMap(game -> game.startTimeProperty()
                .map(startTime -> liveReplayService.canWatchReplay(game))
                .orElse(false)
                .map(canWatch -> canWatch ? (EventHandler<ActionEvent>) event -> liveReplayService.runLiveReplay(game.getId()) : (EventHandler<ActionEvent>) event -> showContextMenu()))
            .when(showing));
  }

  @Override
  public void onShow() {
    GameBean game = this.game.get();
    OffsetDateTime startTime = game == null ? null : game.getStartTime();
    if (startTime != null) {
      checkGameTimeline(startTime);
    }
  }

  @Override
  public void onHide() {
    watchTimeTimeline.stop();
  }

  public void setGame(GameBean game) {
    this.game.set(game);
  }

  private void checkGameTimeline(OffsetDateTime startTime) {
    watchTimeTimeline.stop();
    if (startTime == null) {
      return;
    }

    if (liveReplayService.canWatchReplay(startTime)) {
      watchButton.setText(i18n.get("game.watch"));
    } else {
      watchTimeTimeline.play();
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
    if (gameBean == null) {
      return;
    }

    contextMenu = contextMenuBuilder.newBuilder()
        .addItem(NotifyMeMenuItem.class, gameBean)
        .addItem(CancelActionNotifyMeMenuItem.class, gameBean)
        .addItem(RunReplayImmediatelyMenuItem.class, gameBean)
        .addItem(CancelActionRunReplayImmediatelyMenuItem.class, gameBean)
        .build();
    contextMenu.show(watchButton.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY());
  }

  private void updateDisplay() {
    if (liveReplayService.canWatchReplay(getGame())) {
      watchTimeTimeline.stop();
      fxApplicationThreadExecutor.execute(() -> watchButton.setText(i18n.get("game.watch")));
      if (contextMenu != null && contextMenu.isShowing()) {
        contextMenu.hide();
      }
    } else {
      String waitDuration = timeService.shortDuration(liveReplayService.getWatchDelayTime(getGame()));
      fxApplicationThreadExecutor.execute(() -> watchButton.setText(i18n.get("game.watchDelayedFormat", waitDuration)));
    }
  }

  private void updateButtonTrackingClass(boolean isTracking) {
    fxApplicationThreadExecutor.execute(() -> watchButton.pseudoClassStateChanged(TRACKABLE_PSEUDO_CLASS, isTracking));
  }

  @VisibleForTesting
  Timeline getWatchTimeTimeline() {
    return watchTimeTimeline;
  }

  @Override
  public Node getRoot() {
    return watchButton;
  }
}
