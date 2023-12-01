package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.discord.JoinDiscordEventHandler;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.LoginService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
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

  private final JoinDiscordEventHandler joinDiscordEventHandler;

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
                                   NotificationService notificationService, UiService uiService,
                                   ThemeService themeService,
                                   NavigationHandler navigationHandler,
                                   ChatService chatService,
                                   WebViewConfigurer webViewConfigurer, CountryFlagService countryFlagService,
                                   EmoticonService emoticonService, ChatPrefs chatPrefs,
                                   NotificationPrefs notificationPrefs,
                                   FxApplicationThreadExecutor fxApplicationThreadExecutor,
                                   JoinDiscordEventHandler joinDiscordEventHandler) {
    super(loginService, chatService, playerService, timeService, i18n, notificationService, uiService, themeService,
          webViewConfigurer, emoticonService, countryFlagService, chatPrefs, notificationPrefs,
          fxApplicationThreadExecutor, navigationHandler);
    this.joinDiscordEventHandler = joinDiscordEventHandler;
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();

    matchmakingChatTabRoot.textProperty().bind(channelName.when(showing));

    chatChannel.when(attached).addListener(((observable, oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.removeUserListener(usersChangeListener);
      }

      if (newValue != null) {
        newValue.addUsersListeners(usersChangeListener);
      }
    }));

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
  public void onDetached() {
    super.onDetached();
    ChatChannel channel = chatChannel.get();
    if (channel != null) {
      channel.removeUserListener(usersChangeListener);
    }
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

  public void onDiscordButtonClicked() {
    joinDiscordEventHandler.onJoin(discordLink.getText());
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
