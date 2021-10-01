package com.faforever.client.teammatchmaking;

import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.MatchmakerQueueBean.MatchingStatus;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowMapPoolEvent;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.PlayerService;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MatchmakingQueueItemController implements Controller<VBox> {

  private final static String QUEUE_I18N_PATTERN = "teammatchmaking.queue.%s";

  private final UserService userService;
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
  MatchmakerQueueBean queue;
  private InvalidationListener queueButtonStateInvalidationListener;
  private InvalidationListener queueStateInvalidationListener;
  private InvalidationListener queuePopulationInvalidationListener;
  private ChangeListener<MatchingStatus> queueMatchStatusChangeListener;

  @Override
  public void initialize() {
    JavaFxUtil.bindManagedToVisible(matchFoundLabel, matchStartingLabel, matchCancelledLabel);

    initializeListeners();

    eventBus.register(this);
    joinLeaveQueueButton.setTextOverrun(OverrunStyle.WORD_ELLIPSIS);
    mapPoolButton.setText(i18n.get("teammatchmaking.mapPool").toUpperCase());
  }

  private void initializeListeners() {
    queueButtonStateInvalidationListener = observable -> setQueueButtonState();
    queueStateInvalidationListener = observable -> JavaFxUtil.runLater(() -> {
      refreshingLabel.setVisible(false);
      joinLeaveQueueButton.setSelected(queue.isJoined());
    });
    queuePopulationInvalidationListener = observable ->
        JavaFxUtil.runLater(() -> playersInQueueLabel.setText(i18n.get("teammatchmaking.playersInQueue", queue.getPlayersInQueue()).toUpperCase()));
    queueMatchStatusChangeListener = (observable, oldValue, newValue) -> {
      disableMatchStatus();
      if (newValue == null) {
        return;
      }
      switch (newValue) {
        case MATCH_FOUND -> matchFoundLabel.setVisible(true);
        case GAME_LAUNCHING -> matchStartingLabel.setVisible(true);
        case MATCH_CANCELLED -> matchCancelledLabel.setVisible(true);
        default -> log.warn("Unexpected matching status: " + newValue);
      }
    };
  }

  @Override
  public VBox getRoot() {
    return queueItemRoot;
  }

  public void setQueue(MatchmakerQueueBean queue) {
    this.queue = queue;
    joinLeaveQueueButton.setText(i18n.getOrDefault(queue.getTechnicalName(), String.format(QUEUE_I18N_PATTERN, queue.getTechnicalName())));
    setQueuePopTimeUpdater(queue);

    JavaFxUtil.addAndTriggerListener(queue.matchingStatusProperty(), new WeakChangeListener<>(queueMatchStatusChangeListener));
    JavaFxUtil.addAndTriggerListener(queue.playersInQueueProperty(), new WeakInvalidationListener(queuePopulationInvalidationListener));
    JavaFxUtil.addAndTriggerListener(teamMatchmakingService.getParty().getMembers(), new WeakInvalidationListener(queueButtonStateInvalidationListener));
    JavaFxUtil.addListener(queue.teamSizeProperty(), new WeakInvalidationListener(queueButtonStateInvalidationListener));
    JavaFxUtil.addListener(teamMatchmakingService.getParty().ownerProperty(), new WeakInvalidationListener(queueButtonStateInvalidationListener));
    JavaFxUtil.addListener(teamMatchmakingService.partyMembersNotReadyProperty(), new WeakInvalidationListener(queueButtonStateInvalidationListener));
    JavaFxUtil.addListener(userService.connectionStateProperty(), new WeakInvalidationListener(queueButtonStateInvalidationListener));
    JavaFxUtil.addAndTriggerListener(queue.joinedProperty(), new WeakInvalidationListener(queueStateInvalidationListener));
  }

  private void setQueueButtonState() {
    boolean disable = userService.getConnectionState() != ConnectionState.CONNECTED
        || teamMatchmakingService.getParty().getMembers().size() > queue.getTeamSize()
        || teamMatchmakingService.partyMembersNotReady()
        || !teamMatchmakingService.getParty().getOwner().equals(playerService.getCurrentPlayer());
    JavaFxUtil.runLater(() -> joinLeaveQueueButton.setDisable(disable));
  }

  private void setQueuePopTimeUpdater(MatchmakerQueueBean queue) {
    Timeline queuePopTimeUpdater = new Timeline(1, new KeyFrame(javafx.util.Duration.seconds(0), (ActionEvent event) -> {
      if (queue.getQueuePopTime() != null) {
        OffsetDateTime now = OffsetDateTime.now();
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

  private void disableMatchStatus() {
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
    eventBus.post(new ShowMapPoolEvent(queue));
  }
}
