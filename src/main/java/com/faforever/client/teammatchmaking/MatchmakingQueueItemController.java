package com.faforever.client.teammatchmaking;

import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.MatchmakerQueueBean.MatchingStatus;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowMapPoolEvent;
import com.faforever.client.player.PlayerService;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
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
  public Label activeGamesLabel;
  public Label queuePopTimeLabel;
  public ToggleButton joinLeaveQueueButton;
  public Label refreshingLabel;
  public Label matchFoundLabel;
  public Label matchStartingLabel;
  public Label matchCancelledLabel;
  public Button mapPoolButton;

  @VisibleForTesting
  private final ObjectProperty<MatchmakerQueueBean> queue = new SimpleObjectProperty<>();
  private final ObservableValue<Boolean> queueJoined = queue.flatMap(MatchmakerQueueBean::joinedProperty).orElse(false);

  private final SimpleChangeListener<Boolean> queueStateInvalidationListener = newValue -> JavaFxUtil.runLater(() -> {
    refreshingLabel.setVisible(false);
    joinLeaveQueueButton.setSelected(newValue);
  });

  @Override
  public void initialize() {
    JavaFxUtil.bindManagedToVisible(matchFoundLabel, matchStartingLabel, matchCancelledLabel);

    eventBus.register(this);
    joinLeaveQueueButton.setTextOverrun(OverrunStyle.WORD_ELLIPSIS);
    mapPoolButton.setText(i18n.get("teammatchmaking.mapPool").toUpperCase());

    ObservableValue<MatchingStatus> matchingStatus = queue.flatMap(MatchmakerQueueBean::matchingStatusProperty);
    matchFoundLabel.visibleProperty().bind(matchingStatus.map(status -> status == MatchingStatus.MATCH_FOUND).orElse(false));
    matchStartingLabel.visibleProperty().bind(matchingStatus.map(status -> status == MatchingStatus.GAME_LAUNCHING).orElse(false));
    matchCancelledLabel.visibleProperty().bind(matchingStatus.map(status -> status == MatchingStatus.MATCH_CANCELLED).orElse(false));

    joinLeaveQueueButton.textProperty()
        .bind(queue.map(MatchmakerQueueBean::getTechnicalName)
            .map(technicalName -> i18n.getOrDefault(technicalName, String.format(QUEUE_I18N_PATTERN, technicalName))));

    playersInQueueLabel.textProperty()
        .bind(queue.flatMap(MatchmakerQueueBean::playersInQueueProperty)
            .map(numPlayers -> i18n.get("teammatchmaking.playersInQueue", numPlayers))
            .map(String::toUpperCase));

    activeGamesLabel.textProperty()
        .bind(queue.flatMap(MatchmakerQueueBean::activeGamesProperty)
            .map(numPlayers -> i18n.get("teammatchmaking.activeGames", numPlayers))
            .map(String::toUpperCase));

    BooleanBinding partyTooBig = Bindings.size(teamMatchmakingService.getParty().getMembers())
        .greaterThan(IntegerBinding.integerExpression(queue.flatMap(MatchmakerQueueBean::teamSizeProperty)));

    BooleanExpression notPartyOwner = BooleanBinding.booleanExpression(teamMatchmakingService.getParty()
        .ownerProperty()
        .map(owner -> !owner.equals(playerService.getCurrentPlayer())));

    joinLeaveQueueButton.disableProperty()
        .bind(userService.ownPlayerProperty().isNull()
            .or(queue.isNull())
            .or(partyTooBig)
            .or(teamMatchmakingService.partyMembersNotReadyProperty())
            .or(notPartyOwner));

    queueJoined.addListener(queueStateInvalidationListener);

    queue.addListener((SimpleChangeListener<MatchmakerQueueBean>) this::setQueuePopTimeUpdater);
  }

  @Override
  public VBox getRoot() {
    return queueItemRoot;
  }

  public void setQueue(MatchmakerQueueBean queue) {
    this.queue.set(queue);
  }

  private void setQueuePopTimeUpdater(MatchmakerQueueBean queue) {
    Timeline queuePopTimeUpdater = new Timeline(1, new KeyFrame(javafx.util.Duration.seconds(0), (ActionEvent event) -> {
      if (queue.getQueuePopTime() != null) {
        OffsetDateTime now = OffsetDateTime.now();
        Duration timeUntilPopQueue = Duration.between(now, queue.getQueuePopTime());
        if (!timeUntilPopQueue.isNegative()) {
          queuePopTimeLabel.setText(i18n.get("teammatchmaking.queuePopTimer", timeUntilPopQueue.toMinutes(), timeUntilPopQueue.toSecondsPart())
              .toUpperCase());
        }
      }
    }), new KeyFrame(javafx.util.Duration.seconds(1)));
    queuePopTimeUpdater.setCycleCount(Timeline.INDEFINITE);
    queuePopTimeUpdater.play();
  }

  public void onJoinLeaveQueueClicked() {
    if (queueJoined.getValue()) {
      teamMatchmakingService.leaveQueue(getQueue());
    } else {
      teamMatchmakingService.joinQueue(getQueue()).thenAccept(success -> {
        if (!success) {
          joinLeaveQueueButton.setSelected(false);
          refreshingLabel.setVisible(false);
        }
      });
    }
    refreshingLabel.setVisible(true);
  }

  public void showMapPool() {
    eventBus.post(new ShowMapPoolEvent(getQueue()));
  }

  public MatchmakerQueueBean getQueue() {
    return queue.get();
  }

  public ObjectProperty<MatchmakerQueueBean> queueProperty() {
    return queue;
  }
}
