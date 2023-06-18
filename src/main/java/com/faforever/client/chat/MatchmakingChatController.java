package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.event.UnreadPartyMessageEvent;
import com.faforever.client.discord.JoinDiscordEvent;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.LoginService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MatchmakingChatController extends AbstractChatTabController {

  public Tab matchmakingChatTabRoot;
  public WebView messagesWebView;
  public TextField messageTextField;
  public TextFlow topicText;
  public Hyperlink discordLink;

  private final ListChangeListener<ChatChannelUser> usersChangeListener = change -> {
    while (change.next()) {
      if (change.wasAdded()) {
        List.copyOf(change.getAddedSubList()).forEach(chatUser -> onPlayerConnected(chatUser.getUsername()));
      } else if (change.wasRemoved()) {
        change.getRemoved().forEach(chatUser -> onPlayerDisconnected(chatUser.getUsername()));
      }
    }
  };

  // TODO cut dependencies
  public MatchmakingChatController(LoginService loginService,
                                   PlayerService playerService, TimeService timeService, I18n i18n,
                                   NotificationService notificationService, UiService uiService, EventBus eventBus,
                                   ChatService chatService,
                                   WebViewConfigurer webViewConfigurer, CountryFlagService countryFlagService,
                                   EmoticonService emoticonService, ChatPrefs chatPrefs,
                                   NotificationPrefs notificationPrefs,
                                   FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    super(loginService, chatService, playerService, timeService, i18n, notificationService, uiService, eventBus, webViewConfigurer, emoticonService, countryFlagService, chatPrefs, notificationPrefs, fxApplicationThreadExecutor);
  }

  @Override
  public void initialize() {
    super.initialize();
    matchmakingChatTabRoot.idProperty().bind(channelName);
    matchmakingChatTabRoot.textProperty().bind(channelName);

    chatChannel.addListener(((observable, oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.removeUserListener(usersChangeListener);
      }

      if (newValue != null) {
        newValue.addUsersListeners(usersChangeListener);
      }
    }));

    String topic = i18n.get("teammatchmaking.chat.topic");
    topicText.getChildren().clear();
    Arrays.stream(topic.split("\\s")).forEach(word -> {
      Label label = new Label(word + " ");
      label.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em;");
      topicText.getChildren().add(label);
    });
    topicText.getChildren().add(discordLink);
  }

  @Override
  public Tab getRoot() {
    return matchmakingChatTabRoot;
  }

  public void closeChannel() {
    chatService.leaveChannel(chatChannel.getValue());
    chatChannel.getValue().removeUserListener(usersChangeListener);
  }

  @Override
  protected TextInputControl messageTextField() {
    return messageTextField;
  }

  @Override
  protected WebView getMessagesWebView() {
    return messagesWebView;
  }

  @Override
  protected void onChatMessage(ChatMessage chatMessage) {
    super.onChatMessage(chatMessage);

    if (!hasFocus()) {
      eventBus.post(new UnreadPartyMessageEvent(chatMessage));
    }
  }

  public void onDiscordButtonClicked() {
    eventBus.post(new JoinDiscordEvent(discordLink.getText()));
  }

  @VisibleForTesting
  void onPlayerDisconnected(String userName) {
    fxApplicationThreadExecutor.execute(() -> onChatMessage(new ChatMessage(Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.groupChat.playerDisconnect", userName), true)));
  }

  @VisibleForTesting
  void onPlayerConnected(String userName) {
    fxApplicationThreadExecutor.execute(() -> onChatMessage(new ChatMessage(Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.groupChat.playerConnect", userName), true)));
  }
}
