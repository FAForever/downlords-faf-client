package com.faforever.client.chat;

import com.faforever.client.discord.JoinDiscordEventHandler;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.teammatchmaking.TeamMatchmakingService;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.text.TextFlow;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static com.faforever.client.chat.ChatService.PARTY_CHANNEL_SUFFIX;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MatchmakingChatController extends AbstractChatTabController {

  private final JoinDiscordEventHandler joinDiscordEventHandler;
  private final TeamMatchmakingService teamMatchmakingService;
  private final I18n i18n;

  public Tab matchmakingChatTabRoot;
  public TextFlow topicText;
  public Hyperlink discordLink;

  public MatchmakingChatController(I18n i18n, ChatService chatService, JoinDiscordEventHandler joinDiscordEventHandler,
                                   TeamMatchmakingService teamMatchmakingService) {
    super(chatService);
    this.joinDiscordEventHandler = joinDiscordEventHandler;
    this.teamMatchmakingService = teamMatchmakingService;
    this.i18n = i18n;
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();

    matchmakingChatTabRoot.textProperty().bind(channelName.when(showing));

    chatChannel.bind(teamMatchmakingService.getParty()
                                           .ownerProperty().flatMap(PlayerInfo::usernameProperty)
                                           .map(username -> "#" + username + PARTY_CHANNEL_SUFFIX)
                                           .map(chatService::getOrCreateChannel)
                                           .when(attached));

    String topic = i18n.get("teammatchmaking.chat.topic");
    List<Label> labels = Arrays.stream(topic.split("\\s")).map(word -> {
      Label label = new Label(word + " ");
      label.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em;");
      return label;
    }).toList();
    topicText.getChildren().setAll(labels);
    topicText.getChildren().add(discordLink);
  }

  @Override
  public Tab getRoot() {
    return matchmakingChatTabRoot;
  }

  public void onDiscordButtonClicked() {
    joinDiscordEventHandler.onJoin(discordLink.getText());
  }
}
