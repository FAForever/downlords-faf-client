package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.PrivatePlayerInfoController;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.LoginService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
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
  public PrivateChatTabController(LoginService loginService,
                                  PlayerService playerService, TimeService timeService, I18n i18n,
                                  NotificationService notificationService, UiService uiService,
                                  ThemeService themeService,
                                  NavigationHandler navigationHandler,
                                  ChatService chatService,
                                  WebViewConfigurer webViewConfigurer, CountryFlagService countryFlagService,
                                  EmoticonService emoticonService, AvatarService avatarService, ChatPrefs chatPrefs,
                                  NotificationPrefs notificationPrefs,
                                  FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    super(loginService, chatService, playerService, timeService, i18n, notificationService, uiService, themeService,
          webViewConfigurer, emoticonService, countryFlagService, chatPrefs, notificationPrefs,
          fxApplicationThreadExecutor, navigationHandler);
    this.avatarService = avatarService;
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();

    JavaFxUtil.bindManagedToVisible(avatarImageView, defaultIconImageView);
    avatarImageView.visibleProperty().bind(avatarImageView.imageProperty().isNotNull().when(showing));
    defaultIconImageView.visibleProperty().bind(avatarImageView.imageProperty().isNull().when(showing));
    userOffline = false;

    privateChatTabRoot.textProperty().bind(channelName.when(attached));
    privatePlayerInfoController.chatUserProperty()
        .bind(chatChannel.map(channel -> chatService.getOrCreateChatUser(channel.getName(), channel.getName()))
                         .when(showing));

    avatarImageView.imageProperty()
        .bind(channelName.map(username -> playerService.getPlayerByNameIfOnline(username).orElse(null))
            .flatMap(PlayerBean::avatarProperty)
            .map(avatarService::loadAvatar)
                         .when(showing));

    chatChannel.when(attached).addListener(((observable, oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.removeUserListener(usersChangeListener);
      }

      if (newValue != null) {
        newValue.addUsersListeners(usersChangeListener);
      }
    }));
  }

  @Override
  public void onDetached() {
    super.onDetached();
    ChatChannel channel = chatChannel.get();
    if (channel != null) {
      channel.removeUserListener(usersChangeListener);
    }
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
    if (playerService.getPlayerByNameIfOnline(chatMessage.username())
        .map(PlayerBean::getSocialStatus)
        .map(FOE::equals)
        .orElse(false) && chatPrefs.isHideFoeMessages()) {
      return;
    }

    super.onChatMessage(chatMessage);

    if (!hasFocus()) {
      setUnread(true);
      incrementUnreadMessagesCount();
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
    fxApplicationThreadExecutor.execute(() -> onChatMessage(new ChatMessage(Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.privateMessage.playerLeft", userName), true)));
  }

  @VisibleForTesting
  void onPlayerConnected(String userName) {
    if (!userOffline || !userName.equals(channelName.getValue())) {
      return;
    }
    userOffline = false;
    fxApplicationThreadExecutor.execute(() -> onChatMessage(new ChatMessage(Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.privateMessage.playerReconnect", userName), true)));
  }
}
