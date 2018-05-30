package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.event.UnreadPrivateMessageEvent;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextInputControl;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Optional;

import static com.faforever.client.player.SocialStatus.FOE;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PrivateChatTabController extends AbstractChatTabController {

  public Tab privateChatTabRoot;
  public WebView messagesWebView;
  public TextInputControl messageTextField;
  public PrivateUserInfoController privateUserInfoController;
  public ScrollPane gameDetailScrollPane;

  private boolean userOffline;

  @Inject
  // TODO cut dependencies
  public PrivateChatTabController(UserService userService,
                                  PreferencesService preferencesService,
                                  PlayerService playerService,
                                  TimeService timeService,
                                  I18n i18n,
                                  ImageUploadService imageUploadService,
                                  NotificationService notificationService,
                                  ReportingService reportingService,
                                  UiService uiService,
                                  AutoCompletionHelper autoCompletionHelper,
                                  EventBus eventBus,
                                  AudioService audioService,
                                  ChatService chatService,
                                  WebViewConfigurer webViewConfigurer,
                                  CountryFlagService countryFlagService) {
    super(webViewConfigurer, userService, chatService, preferencesService, playerService, audioService,
        timeService, i18n, imageUploadService, notificationService, reportingService, uiService, autoCompletionHelper,
        eventBus, countryFlagService);
  }


  boolean isUserOffline() {
    return userOffline;
  }

  @Override
  public Tab getRoot() {
    return privateChatTabRoot;
  }

  @Override
  public void setReceiver(String username) {
    super.setReceiver(username);
    privateChatTabRoot.setId(username);
    privateChatTabRoot.setText(username);

    ChatUser chatUser = chatService.getOrCreateChatUser(username);
    privateUserInfoController.setChatUser(chatUser);
  }

  public void initialize() {
    super.initialize();
    JavaFxUtil.fixScrollSpeed(gameDetailScrollPane);
    userOffline = false;
    chatService.addChatUsersByNameListener(change -> {
      if (change.wasRemoved()) {
        onPlayerDisconnected(change.getKey());
      }
      if (change.wasAdded()) {
        onPlayerConnected(change.getKey());
      }
    });
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
    Optional<Player> playerOptional = playerService.getPlayerForUsername(chatMessage.getUsername());
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    if (playerOptional.isPresent() && playerOptional.get().getSocialStatus() == FOE && chatPrefs.getHideFoeMessages()) {
      return;
    }

    super.onChatMessage(chatMessage);

    if (!hasFocus()) {
      audioService.playPrivateMessageSound();
      showNotificationIfNecessary(chatMessage);
      setUnread(true);
      incrementUnreadMessagesCount(1);
      eventBus.post(new UnreadPrivateMessageEvent(chatMessage));
    }
  }

  @VisibleForTesting
  void onPlayerDisconnected(String userName) {
    if (!userName.equals(getReceiver())) {
      return;
    }
    userOffline = true;
    Platform.runLater(() -> onChatMessage(new ChatMessage(userName, Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.privateMessage.playerLeft", userName), true)));
  }

  @VisibleForTesting
  void onPlayerConnected(String userName) {
    if (!userOffline || !userName.equals(getReceiver())) {
      return;
    }
    userOffline = false;
    Platform.runLater(() -> onChatMessage(new ChatMessage(userName, Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.privateMessage.playerReconnect", userName), true)));
  }
}
