package com.faforever.client.teammatchmaking;

import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.CountryFlagService;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
  public Label playersInQueueLabel;

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

    playersInQueueLabel.textProperty().bind(Bindings.createStringBinding(
        () -> i18n.get("teammatchmaking.playersInQueue", queue.getPlayersInQueue()),
        queue.playersInQueueProperty()));
    //TODO hook into all queue pop times and start animation
  }

  public void onJoinQueueClicked(ActionEvent actionEvent) {
    //TODO
  }
}
