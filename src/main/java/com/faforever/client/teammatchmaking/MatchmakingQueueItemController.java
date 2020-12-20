package com.faforever.client.teammatchmaking;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowMapPoolEvent;
import com.faforever.client.player.PlayerService;
import com.google.common.eventbus.EventBus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

import static javafx.beans.binding.Bindings.createBooleanBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MatchmakingQueueItemController implements Controller<VBox> {

  private final static String QUEUE_I18N_PATTERN = "teammatchmaking.queue.%s.%s";

  private final PlayerService playerService;
  private final TeamMatchmakingService teamMatchmakingService;
  private final I18n i18n;
  private final EventBus eventBus;

  public VBox queueItemRoot;
  public Label playersInQueueLabel;
  public Label queuePopTimeLabel;
  public ToggleButton joinLeaveQueueButton;
  public Label refreshingLabel;
  public Label matchFoundLabel;
  public Label matchStartingLabel;
  public Label matchCancelledLabel;
  public Button mapPoolButton;

  @VisibleForTesting
  protected MatchmakingQueue queue;

  public MatchmakingQueueItemController(PlayerService playerService, TeamMatchmakingService teamMatchmakingService, I18n i18n, EventBus eventBus) {
    this.playerService = playerService;
    this.teamMatchmakingService = teamMatchmakingService;
    this.i18n = i18n;
    this.eventBus = eventBus;
  }

  @Override
  public void initialize() {
    eventBus.register(this);
    joinLeaveQueueButton.widthProperty().addListener((observable, oldValue, newValue) -> {
      if ((double) newValue > 150.0) {
        joinLeaveQueueButton.setText(
            i18n.get(String.format(QUEUE_I18N_PATTERN, queue.getQueueName(), "fullName")));
      } else {
        joinLeaveQueueButton.setText(
            i18n.get(String.format(QUEUE_I18N_PATTERN, queue.getQueueName(), "shortName")));
      }
    });
  }

  @Override
  public VBox getRoot() {
    return queueItemRoot;
  }

  public void setQueue(MatchmakingQueue queue) {
    this.queue = queue;

    playersInQueueLabel.textProperty().bind(createStringBinding(
        () -> i18n.get("teammatchmaking.playersInQueue", queue.getPlayersInQueue()).toUpperCase(),
        queue.playersInQueueProperty()));

    JavaFxUtil.bindManagedToVisible(matchFoundLabel, matchStartingLabel, matchCancelledLabel);
    queue.matchingStatusProperty().addListener((observable, oldValue, newValue) -> {
      disableMatchStatus();
      if (newValue == null) {
        return;
      }
      switch (newValue) {
        case MATCH_FOUND -> matchFoundLabel.setVisible(true);
        case GAME_LAUNCHING -> matchStartingLabel.setVisible(true);
        case MATCH_CANCELLED -> matchCancelledLabel.setVisible(true);
      }
    });

    joinLeaveQueueButton.disableProperty().bind(createBooleanBinding(
        () -> teamMatchmakingService.getParty().getMembers().size() > queue.getTeamSize()
            || !teamMatchmakingService.getParty().getOwner().equals(playerService.getCurrentPlayer().orElse(null)),
        teamMatchmakingService.getParty().getMembers(), queue.teamSizeProperty()
    ));
    queue.joinedProperty().addListener(observable -> refreshingLabel.setVisible(false));
    queue.joinedProperty().addListener(observable -> joinLeaveQueueButton.setSelected(queue.isJoined()));

    mapPoolButton.setText(i18n.get("teammatchmaking.mapPool").toUpperCase());

    Timeline queuePopTimeUpdater = new Timeline(1, new KeyFrame(javafx.util.Duration.seconds(0), (ActionEvent event) -> {
      if (queue.getQueuePopTime() != null) {
        Instant now = Instant.now();
        Duration timeUntilPopQueue = Duration.between(now, queue.getQueuePopTime());
        if (!timeUntilPopQueue.isNegative()) {
          queuePopTimeLabel.setText(i18n.get("teammatchmaking.queuePopTimer",
              timeUntilPopQueue.toMinutes(),
              timeUntilPopQueue.toSecondsPart()).toUpperCase());
        }
      }
    }), new KeyFrame(javafx.util.Duration.seconds(1)));
    queuePopTimeUpdater.setCycleCount(Timeline.INDEFINITE);
    queuePopTimeUpdater.play();
  }

  public void disableMatchStatus() {
    matchFoundLabel.setVisible(false);
    matchStartingLabel.setVisible(false);
    matchCancelledLabel.setVisible(false);
  }

  public void onJoinLeaveQueueClicked(ActionEvent actionEvent) {
    if (queue.isJoined()) {
      teamMatchmakingService.leaveQueue(queue);
    } else {
      boolean success = teamMatchmakingService.joinQueue(queue);
      if (!success) {
        joinLeaveQueueButton.setSelected(false);
        refreshingLabel.setVisible(false);
      }
    }
    refreshingLabel.setVisible(true);
  }

  public void showMapPool(ActionEvent actionEvent) {
    eventBus.post(new ShowMapPoolEvent(queue.getQueueId()));
  }
}
