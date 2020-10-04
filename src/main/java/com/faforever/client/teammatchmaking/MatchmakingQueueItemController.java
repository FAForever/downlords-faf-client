package com.faforever.client.teammatchmaking;

import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

import static javafx.beans.binding.Bindings.createBooleanBinding;
import static javafx.beans.binding.Bindings.createStringBinding;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MatchmakingQueueItemController implements Controller<Node> {

  private final static String QUEUE_I18N_PATTERN = "teammatchmaking.queue.%s.%s";

  private final CountryFlagService countryFlagService;
  private final AvatarService avatarService;
  private final PlayerService playerService;
  private final TeamMatchmakingService teamMatchmakingService;
  private final UiService uiService;
  private final ChatService chatService;
  private final I18n i18n;

  @FXML
  public Node queueItemRoot;
  @FXML
  public Label playersInQueueLabel;
  @FXML
  public Label queuePopTimeLabel;
  @FXML
  public ToggleButton joinLeaveQueueButton;
  @FXML
  public Label refreshingLabel;
  @FXML
  public Label matchFoundLabel;
  @FXML
  public Label matchStartingLabel;
  @FXML
  public Label matchCancelledLabel;


  private Timeline queuePopTimeUpdater;

  private MatchmakingQueue queue;

  public MatchmakingQueueItemController(CountryFlagService countryFlagService, AvatarService avatarService, PlayerService playerService, TeamMatchmakingService teamMatchmakingService, UiService uiService, ChatService chatService, I18n i18n) {
    this.countryFlagService = countryFlagService;
    this.avatarService = avatarService;
    this.playerService = playerService;
    this.teamMatchmakingService = teamMatchmakingService;
    this.uiService = uiService;
    this.chatService = chatService;
    this.i18n = i18n;
  }

  @Override
  public void initialize() {
    joinLeaveQueueButton.widthProperty().addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        if ((double) newValue > 150.0) {
          joinLeaveQueueButton.setText(
              i18n.get(String.format(QUEUE_I18N_PATTERN, queue.getQueueName(), "fullName")));
        } else {
          joinLeaveQueueButton.setText(
              i18n.get(String.format(QUEUE_I18N_PATTERN, queue.getQueueName(), "shortName")));
        }
      }
    });
  }

  @Override
  public Node getRoot() {
    return queueItemRoot;
  }

  void setQueue(MatchmakingQueue queue) {
    this.queue = queue;

    playersInQueueLabel.textProperty().bind(createStringBinding(
        () -> playersInQueueLabel.getStyleClass().contains("uppercase") ?
            i18n.get("teammatchmaking.playersInQueue", queue.getPlayersInQueue()).toUpperCase() :
            i18n.get("teammatchmaking.playersInQueue", queue.getPlayersInQueue()),
        queue.playersInQueueProperty()));

    matchFoundLabel.visibleProperty().bind(matchFoundLabel.managedProperty());
    matchStartingLabel.visibleProperty().bind(matchStartingLabel.managedProperty());
    matchCancelledLabel.visibleProperty().bind(matchCancelledLabel.managedProperty());
    queue.matchingStatusProperty().addListener((observable, oldValue, newValue) -> {
      disableMatchStatus();
      if (newValue == null) {
        return;
      }
      switch (newValue) {
        case MATCH_FOUND:
          matchFoundLabel.setManaged(true);
          break;
        case GAME_LAUNCHING:
          matchStartingLabel.setManaged(true);
          break;
        case MATCH_CANCELLED:
          matchCancelledLabel.setManaged(true);
          break;
      }
    });

    joinLeaveQueueButton.disableProperty().bind(createBooleanBinding(
        () -> teamMatchmakingService.getParty().getMembers().size() > queue.getTeamSize()
            || !teamMatchmakingService.getParty().getOwner().equals(playerService.getCurrentPlayer().orElse(null)),
        teamMatchmakingService.getParty().getMembers()
    ));

    queue.joinedProperty().addListener(observable -> refreshingLabel.setVisible(false));
    queue.joinedProperty().addListener(observable -> joinLeaveQueueButton.setSelected(queue.isJoined()));

    queuePopTimeUpdater = new Timeline(1, new KeyFrame(javafx.util.Duration.seconds(0), (ActionEvent event) -> {
      if (queue.getQueuePopTime() != null) {
        Instant now = Instant.now();
        Duration timeUntilPopQueue = Duration.between(now, queue.getQueuePopTime());
        if (!timeUntilPopQueue.isNegative()) {
          String formatted = i18n.get("teammatchmaking.queuePopTimer",
              timeUntilPopQueue.toMinutes(),
              timeUntilPopQueue.toSecondsPart());
          queuePopTimeLabel.setText(
              queuePopTimeLabel.getStyleClass().contains("uppercase") ? formatted.toUpperCase() : formatted);
          return;
        }
      }
    }), new KeyFrame(javafx.util.Duration.seconds(1)));
    queuePopTimeUpdater.setCycleCount(Timeline.INDEFINITE);
    queuePopTimeUpdater.play();
  }

  public void disableMatchStatus() {
    matchFoundLabel.setManaged(false);
    matchStartingLabel.setManaged(false);
    matchCancelledLabel.setManaged(false);
  }

  public void onJoinLeaveQueueClicked(ActionEvent actionEvent) {
    if (queue.isJoined()) {
      teamMatchmakingService.leaveQueue(queue);
    } else {
      teamMatchmakingService.joinQueue(queue);
    }
    refreshingLabel.setVisible(true);
  }
}
