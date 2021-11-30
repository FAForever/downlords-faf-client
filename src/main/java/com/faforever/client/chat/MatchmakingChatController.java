package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.event.UnreadPartyMessageEvent;
import com.faforever.client.discord.JoinDiscordEvent;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.collections.MapChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextInputControl;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MatchmakingChatController extends AbstractChatTabController {

  public Tab matchmakingChatTabRoot;
  public WebView messagesWebView;
  public TextFlow topicText;
  public Hyperlink discordLink;

  private ChatChannel channel;
  private MapChangeListener<String, ChatChannelUser> usersChangeListener;

  // TODO cut dependencies
  public MatchmakingChatController(UserService userService,
                                   PreferencesService preferencesService,
                                   PlayerService playerService,
                                   TimeService timeService,
                                   I18n i18n,
                                   ImageUploadService imageUploadService,
                                   NotificationService notificationService,
                                   ReportingService reportingService,
                                   UiService uiService,
                                   EventBus eventBus,
                                   AudioService audioService,
                                   ChatService chatService,
                                   WebViewConfigurer webViewConfigurer,
                                   CountryFlagService countryFlagService,
                                   ChatUserService chatUserService) {
    super(webViewConfigurer, userService, chatService, preferencesService, playerService, audioService,
        timeService, i18n, imageUploadService, notificationService, reportingService, uiService,
        eventBus, countryFlagService, chatUserService);
  }

  @Override
  public Tab getRoot() {
    return matchmakingChatTabRoot;
  }

  public void setChannel(String partyName) {
    channel = chatService.getOrCreateChannel(partyName);
    chatService.joinChannel(partyName);
    setReceiver(partyName);
    matchmakingChatTabRoot.setId(partyName);
    matchmakingChatTabRoot.setText(partyName);
    String topic = i18n.get("teammatchmaking.chat.topic");
    topicText.getChildren().clear();
    Arrays.stream(topic.split("\\s"))
        .forEach(word -> {
          Label label = new Label(word + " ");
          label.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em;");
            topicText.getChildren().add(label);
        });
    topicText.getChildren().add(discordLink);

    usersChangeListener = change -> {
      if (change.wasAdded()) {
        onPlayerConnected(change.getValueAdded().getUsername());
      } else if (change.wasRemoved()) {
        onPlayerDisconnected(change.getValueRemoved().getUsername());
      }
    };
    chatService.addUsersListener(partyName, usersChangeListener);
  }

  public void closeChannel() {
    chatService.leaveChannel(channel.getName());
    chatService.removeUsersListener(channel.getName(), usersChangeListener);
  }

  @Override
  protected WebView getMessagesWebView() {
    return messagesWebView;
  }

  @Override
  public void onChatMessage(ChatMessage chatMessage) {
    super.onChatMessage(chatMessage);

    if (!hasFocus()) {
      eventBus.post(new UnreadPartyMessageEvent(chatMessage));
    }
  }

  public void onDiscordButtonClicked() {
    eventBus.post(new JoinDiscordEvent());
  }

  @VisibleForTesting
  void onPlayerDisconnected(String userName) {
    JavaFxUtil.runLater(() -> onChatMessage(new ChatMessage(userName, Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.groupChat.playerDisconnect", userName), true)));
  }

  @VisibleForTesting
  void onPlayerConnected(String userName) {
    JavaFxUtil.runLater(() -> onChatMessage(new ChatMessage(userName, Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.groupChat.playerConnect", userName), true)));
  }
}
