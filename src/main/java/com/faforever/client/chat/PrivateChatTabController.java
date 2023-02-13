package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.avatar.AvatarService;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.event.UnreadPrivateMessageEvent;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.PrivatePlayerInfoController;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.WeakMapChangeListener;
import javafx.event.Event;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
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

  private final AvatarService avatarService;

  public Tab privateChatTabRoot;
  public ImageView avatarImageView;
  public Region defaultIconImageView;
  public WebView messagesWebView;
  public TextInputControl messageTextField;
  public PrivatePlayerInfoController privatePlayerInfoController;
  public ScrollPane gameDetailScrollPane;

  private final MapChangeListener<String, ChatChannelUser> chatUsersByNameListener = this::handlerPlayerChange;

  private boolean userOffline;

  @Inject
  // TODO cut dependencies
  public PrivateChatTabController(UserService userService,
                                  PreferencesService preferencesService,
                                  PlayerService playerService,
                                  TimeService timeService,
                                  I18n i18n,
                                  NotificationService notificationService,
                                  UiService uiService,
                                  EventBus eventBus,
                                  AudioService audioService,
                                  ChatService chatService,
                                  WebViewConfigurer webViewConfigurer,
                                  CountryFlagService countryFlagService,
                                  EmoticonService emoticonService,
                                  AvatarService avatarService) {
    super(userService, chatService, preferencesService, playerService, audioService, timeService,
        i18n, notificationService, uiService, eventBus, webViewConfigurer, emoticonService,
        countryFlagService);
    this.avatarService = avatarService;
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
    playerService.getPlayerByNameIfOnline(username).ifPresent(player ->
        avatarImageView.imageProperty().bind(player.avatarProperty().map(avatarService::loadAvatar)));
    ChatChannelUser chatUser = chatService.getOrCreateChatUser(username, username, false);
    privatePlayerInfoController.setChatUser(chatUser);
  }

  @Override
  protected void onClosed(Event event) {
    privatePlayerInfoController.dispose();
  }

  public void initialize() {
    super.initialize();
    JavaFxUtil.bindManagedToVisible(avatarImageView, defaultIconImageView);
    avatarImageView.visibleProperty().bind(avatarImageView.imageProperty().isNotNull());
    defaultIconImageView.visibleProperty().bind(avatarImageView.imageProperty().isNull());
    JavaFxUtil.fixScrollSpeed(gameDetailScrollPane);
    userOffline = false;

    chatService.addChatUsersByNameListener(new WeakMapChangeListener<>(chatUsersByNameListener));
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
    Optional<PlayerBean> playerOptional = playerService.getPlayerByNameIfOnline(chatMessage.getUsername());
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    if (playerOptional.isPresent() && playerOptional.get().getSocialStatus() == FOE && chatPrefs.getHideFoeMessages()) {
      return;
    }

    super.onChatMessage(chatMessage);

    if (!hasFocus()) {
      audioService.playPrivateMessageSound();
      showNotificationIfNecessary(chatMessage);
      setUnread(true);
      incrementUnreadMessagesCount();
      eventBus.post(new UnreadPrivateMessageEvent(chatMessage));
    }
  }

  private void handlerPlayerChange(Change<? extends String, ? extends ChatChannelUser> change) {
    if (change.wasRemoved()) {
      onPlayerDisconnected(change.getKey());
    }
    if (change.wasAdded()) {
      onPlayerConnected(change.getKey());
    }
  }

  @VisibleForTesting
  void onPlayerDisconnected(String userName) {
    if (!userName.equals(getReceiver())) {
      return;
    }
    userOffline = true;
    JavaFxUtil.runLater(() -> onChatMessage(new ChatMessage(userName, Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.privateMessage.playerLeft", userName), true)));
  }

  @VisibleForTesting
  void onPlayerConnected(String userName) {
    if (!userOffline || !userName.equals(getReceiver())) {
      return;
    }
    userOffline = false;
    JavaFxUtil.runLater(() -> onChatMessage(new ChatMessage(userName, Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.privateMessage.playerReconnect", userName), true)));
  }
}
