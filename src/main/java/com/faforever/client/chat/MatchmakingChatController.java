package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.event.UnreadPartyMessageEvent;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TextInputControl;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.Instant;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MatchmakingChatController extends AbstractChatTabController {

  public Tab matchmakingChatTabRoot;
  public WebView messagesWebView;
  public TextInputControl messageTextField;

  private Channel channel;
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
                                   CountryFlagService countryFlagService) {
    super(webViewConfigurer, userService, chatService, preferencesService, playerService, audioService,
        timeService, i18n, imageUploadService, notificationService, reportingService, uiService,
        eventBus, countryFlagService);
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
  protected TextInputControl messageTextField() {
    return messageTextField;
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

  @VisibleForTesting
  void onPlayerDisconnected(String userName) {
    Platform.runLater(() -> onChatMessage(new ChatMessage(userName, Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.groupChat.playerDisconnect", userName), true)));
  }

  @VisibleForTesting
  void onPlayerConnected(String userName) {
    Platform.runLater(() -> onChatMessage(new ChatMessage(userName, Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.groupChat.playerConnect", userName), true)));
  }
}
