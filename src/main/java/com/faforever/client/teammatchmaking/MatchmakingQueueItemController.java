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
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MatchmakingQueueItemController implements Controller<Node> {

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
  public Label queuenameLabel;
  @FXML
  public Label partiesInQueueLabel;
  @FXML
  public Label teamSizeLabel;
  @FXML
  public ImageView leagueImageView;
  @FXML
  public Label queuePopTimeLabel;

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

  }

  @Override
  public Node getRoot() {
    return queueItemRoot;
  }

  void setQueue(MatchmakingQueue queue) {
    this.queue = queue;

    queuenameLabel.textProperty().bind(queue.queueNameProperty());

    teamSizeLabel.textProperty().bind(Bindings.createStringBinding(
        () -> i18n.get("teammatchmaking.teamSize", queue.getTeamSize()),
        queue.teamSizeProperty()));
    partiesInQueueLabel.textProperty().bind(Bindings.createStringBinding(
        () -> i18n.get("teammatchmaking.partiesInQueue", queue.getPartiesInQueue()),
        queue.partiesInQueueProperty()));


//    leagueImageView.imageProperty().bind(createObjectBinding(() -> avatarService.loadAvatar(player.getAvatarUrl()), player.avatarUrlProperty()));
    leagueImageView.setImage(avatarService.loadAvatar("https://content.faforever.com/faf/avatars/ICE_Test.png"));


    queuePopTimeLabel.visibleProperty().bind(queue.queuePopTimeProperty().isNotNull());
    queuePopTimeUpdater = new Timeline(1, new KeyFrame(javafx.util.Duration.seconds(0), (ActionEvent event) -> {
      if (queue.getQueuePopTime() != null) {
        Instant now = Instant.now();
        Duration timeUntilPopQueue = Duration.between(now, queue.getQueuePopTime());
        if (!timeUntilPopQueue.isNegative()) {
          String formatted = i18n.get("teammatchmaking.queuePopTimer",
              timeUntilPopQueue.toMinutes(),
              timeUntilPopQueue.toSecondsPart());
          queuePopTimeLabel.setText(formatted);
          return;
        }
      }
    }), new KeyFrame(javafx.util.Duration.seconds(1)));
    queuePopTimeUpdater.setCycleCount(Timeline.INDEFINITE);
    queuePopTimeUpdater.play();
  }

  public void onJoinQueueClicked(ActionEvent actionEvent) {
    //TODO
  }
}
