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
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.event.Event;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
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
  public TextField messageTextField;
  public PrivatePlayerInfoController privatePlayerInfoController;
  public ScrollPane gameDetailScrollPane;

  private final ListChangeListener<ChatChannelUser> usersChangeListener = this::handlerPlayerChange;

  private boolean userOffline;

  @Autowired
  // TODO cut dependencies
  public PrivateChatTabController(UserService userService, PreferencesService preferencesService,
                                  PlayerService playerService, TimeService timeService, I18n i18n,
                                  NotificationService notificationService, UiService uiService, EventBus eventBus,
                                  AudioService audioService, ChatService chatService,
                                  WebViewConfigurer webViewConfigurer, CountryFlagService countryFlagService,
                                  EmoticonService emoticonService, AvatarService avatarService, ChatPrefs chatPrefs,
                                  NotificationPrefs notificationPrefs) {
    super(userService, chatService, preferencesService, playerService, audioService, timeService, i18n, notificationService, uiService, eventBus, webViewConfigurer, emoticonService, countryFlagService, chatPrefs, notificationPrefs);
    this.avatarService = avatarService;
  }

  public void initialize() {
    super.initialize();

    ObservableValue<Boolean> showing = getRoot().selectedProperty()
        .and(BooleanExpression.booleanExpression(getRoot().tabPaneProperty().flatMap(JavaFxUtil::showingProperty)));

    JavaFxUtil.bindManagedToVisible(avatarImageView, defaultIconImageView);
    avatarImageView.visibleProperty().bind(avatarImageView.imageProperty().isNotNull());
    defaultIconImageView.visibleProperty().bind(avatarImageView.imageProperty().isNull());
    userOffline = false;

    privateChatTabRoot.idProperty().bind(channelName);
    privateChatTabRoot.textProperty().bind(channelName);
    privatePlayerInfoController.chatUserProperty()
        .bind(chatChannel.map(channel -> chatService.getOrCreateChatUser(channel.getName(), channel.getName())));

    avatarImageView.imageProperty()
        .bind(channelName.map(username -> playerService.getPlayerByNameIfOnline(username).orElse(null))
            .flatMap(PlayerBean::avatarProperty)
            .map(avatarService::loadAvatar)
            .when(showing));

    chatChannel.addListener(((observable, oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.removeUserListener(usersChangeListener);
      }

      if (newValue != null) {
        newValue.addUsersListeners(usersChangeListener);
      }
    }));
  }

  boolean isUserOffline() {
    return userOffline;
  }

  @Override
  public Tab getRoot() {
    return privateChatTabRoot;
  }

  @Override
  protected void onClosed(Event event) {
    super.onClosed(event);
    chatChannel.getValue().removeUserListener(usersChangeListener);
    privatePlayerInfoController.dispose();
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
    Optional<PlayerBean> playerOptional = playerService.getPlayerByNameIfOnline(chatMessage.username());

    if (playerOptional.isPresent() && playerOptional.get().getSocialStatus() == FOE && chatPrefs.isHideFoeMessages()) {
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

  private void handlerPlayerChange(Change<? extends ChatChannelUser> change) {
    while (change.next()) {
      if (change.wasRemoved()) {
        change.getRemoved().forEach(chatUser -> onPlayerDisconnected(chatUser.getUsername()));
      }
      if (change.wasAdded()) {
        List.copyOf(change.getAddedSubList()).forEach(chatUser -> onPlayerConnected(chatUser.getUsername()));
      }
    }
  }

  @VisibleForTesting
  void onPlayerDisconnected(String userName) {
    if (!userName.equals(channelName.getValue())) {
      return;
    }
    userOffline = true;
    JavaFxUtil.runLater(() -> onChatMessage(new ChatMessage(Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.privateMessage.playerLeft", userName), true)));
  }

  @VisibleForTesting
  void onPlayerConnected(String userName) {
    if (!userOffline || !userName.equals(channelName.getValue())) {
      return;
    }
    userOffline = false;
    JavaFxUtil.runLater(() -> onChatMessage(new ChatMessage(Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.privateMessage.playerReconnect", userName), true)));
  }
}
