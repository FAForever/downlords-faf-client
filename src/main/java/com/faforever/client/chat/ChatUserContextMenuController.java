package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.fx.WindowController;
import com.faforever.client.game.GameService;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Objects;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.chat.SocialStatus.FOE;
import static com.faforever.client.chat.SocialStatus.FRIEND;
import static com.faforever.client.chat.SocialStatus.SELF;
import static com.faforever.client.fx.WindowController.WindowButtonType.CLOSE;
import static java.util.Locale.US;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class ChatUserContextMenuController implements Controller<ContextMenu> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final ChatService chatService;
  private final PreferencesService preferencesService;
  private final PlayerService playerService;
  private final ReplayService replayService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final EventBus eventBus;
  private final JoinGameHelper joinGameHelper;
  private final AvatarService avatarService;
  private final UiService uiService;
  public ComboBox<AvatarBean> avatarComboBox;
  public CustomMenuItem avatarPickerMenuItem;
  public MenuItem sendPrivateMessageItem;
  public SeparatorMenuItem socialSeparator;
  public MenuItem removeCustomColorItem;
  public CustomMenuItem colorPickerMenuItem;
  public ColorPicker colorPicker;
  public MenuItem followPlayerItem;
  public MenuItem joinGameItem;
  public MenuItem addFriendItem;
  public MenuItem removeFriendItem;
  public MenuItem addFoeItem;
  public MenuItem removeFoeItem;
  public MenuItem watchGameItem;
  public MenuItem viewReplaysItem;
  public MenuItem inviteItem;
  public SeparatorMenuItem moderatorActionSeparator;
  public MenuItem kickItem;
  public MenuItem banItem;
  public ContextMenu chatUserContextMenuRoot;
  public MenuItem showUserInfo;
  private Player player;

  @Inject
  public ChatUserContextMenuController(UserService userService, ChatService chatService, PreferencesService preferencesService, PlayerService playerService, GameService gameService, ReplayService replayService, NotificationService notificationService, I18n i18n, EventBus eventBus, JoinGameHelper joinGameHelper, AvatarService avatarService, UiService uiService) {
    this.chatService = chatService;
    this.preferencesService = preferencesService;
    this.playerService = playerService;
    this.replayService = replayService;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.eventBus = eventBus;
    this.joinGameHelper = joinGameHelper;
    this.avatarService = avatarService;
    this.uiService = uiService;
  }

  public void initialize() {
    avatarComboBox.setCellFactory(param -> avatarCell());
    avatarComboBox.setButtonCell(avatarCell());
  }

  @NotNull
  private StringListCell<AvatarBean> avatarCell() {
    return new StringListCell<>(
        AvatarBean::getDescription,
        avatarBean -> {
          URL url = avatarBean.getUrl();
          if (url == null) {
            return null;
          }
          return new ImageView(avatarService.loadAvatar(url.toString()));
        });
  }

  ContextMenu getContextMenu() {
    return chatUserContextMenuRoot;
  }

  public void setPlayer(Player player) {
    this.player = player;
    showUserInfo.visibleProperty().bind(Bindings.createBooleanBinding(() -> player.getId() > 0, player.idProperty()));

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    String lowerCaseUsername = player.getUsername().toLowerCase(US);
    colorPicker.setValue(chatPrefs.getUserToColor().getOrDefault(lowerCaseUsername, null));

    colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
      String lowerUsername = player.getUsername().toLowerCase(US);
      if (newValue == null) {
        chatPrefs.getUserToColor().remove(lowerUsername);
      } else {
        chatPrefs.getUserToColor().put(lowerUsername, newValue);
      }
      ChatUser chatUser = chatService.getOrCreateChatUser(lowerUsername);
      chatUser.setColor(newValue);
      chatUserContextMenuRoot.hide();
    });

    removeCustomColorItem.visibleProperty().bind(chatPrefs.chatColorModeProperty().isEqualTo(CUSTOM)
        .and(colorPicker.valueProperty().isNotNull()));
    colorPickerMenuItem.visibleProperty().bind(chatPrefs.chatColorModeProperty()
        .isEqualTo(CUSTOM));

    if (player.getSocialStatus() != SocialStatus.SELF) {
      avatarPickerMenuItem.setVisible(false);
    } else {
      loadAvailableAvatars();
    }

    kickItem.visibleProperty().bind(player.socialStatusProperty().isNotEqualTo(SELF));
    banItem.visibleProperty().bind(player.socialStatusProperty().isNotEqualTo(SELF));
    moderatorActionSeparator.visibleProperty().bind(player.socialStatusProperty().isNotEqualTo(SELF));

    sendPrivateMessageItem.visibleProperty().bind(player.socialStatusProperty().isNotEqualTo(SELF));

    addFriendItem.visibleProperty().bind(
        player.socialStatusProperty().isNotEqualTo(FRIEND).and(player.socialStatusProperty().isNotEqualTo(SELF))
    );
    removeFriendItem.visibleProperty().bind(player.socialStatusProperty().isEqualTo(FRIEND));
    addFoeItem.visibleProperty().bind(player.socialStatusProperty().isNotEqualTo(FOE).and(player.socialStatusProperty().isNotEqualTo(SELF)));
    removeFoeItem.visibleProperty().bind(player.socialStatusProperty().isEqualTo(FOE));

    followPlayerItem.visibleProperty().bind(player.socialStatusProperty().isNotEqualTo(SELF).and(player.chatOnlyProperty().isEqualTo(new SimpleBooleanProperty(false))));
    joinGameItem.visibleProperty().bind(player.socialStatusProperty().isNotEqualTo(SELF)
        .and(player.statusProperty().isEqualTo(PlayerStatus.LOBBYING)
            .or(player.statusProperty().isEqualTo(PlayerStatus.HOSTING))));
    watchGameItem.visibleProperty().bind(player.statusProperty().isEqualTo(PlayerStatus.PLAYING));
    inviteItem.visibleProperty().bind(player.socialStatusProperty().isNotEqualTo(SELF)
        .and(player.statusProperty().isNotEqualTo(PlayerStatus.PLAYING)));

    socialSeparator.visibleProperty().bind(addFriendItem.visibleProperty().or(
        removeFriendItem.visibleProperty().or(
            addFoeItem.visibleProperty().or(
                removeFoeItem.visibleProperty()))));
  }

  private void loadAvailableAvatars() {
    avatarService.getAvailableAvatars().thenAccept(avatars -> {
      ObservableList<AvatarBean> items = FXCollections.observableArrayList(avatars);
      items.add(0, new AvatarBean(null, i18n.get("chat.userContext.noAvatar")));


      String currentAvatarUrl = player.getAvatarUrl();
      Platform.runLater(() -> {
        avatarComboBox.setItems(items);
        avatarComboBox.getSelectionModel().select(items.stream()
            .filter(avatarBean -> Objects.equals(Objects.toString(avatarBean.getUrl(), null), currentAvatarUrl))
            .findFirst()
            .orElse(null));

        // Only after the box has been populated and we selected the current value, we add the listener.
        // Otherwise the code above already triggers a changeAvatar()
        avatarComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
          player.setAvatarTooltip(newValue == null ? null : newValue.getDescription());
          player.setAvatarUrl(newValue == null ? null : Objects.toString(newValue.getUrl(), null));
          avatarService.changeAvatar(newValue);
        });
      });

    });
  }

  public void onUserInfo() {
    UserInfoWindowController userInfoWindowController = uiService.loadFxml("theme/user_info_window.fxml");
    userInfoWindowController.setPlayer(player);

    Stage userInfoWindow = new Stage(StageStyle.TRANSPARENT);
    userInfoWindow.initModality(Modality.NONE);
    userInfoWindow.initOwner(chatUserContextMenuRoot.getOwnerWindow());

    WindowController windowController = uiService.loadFxml("theme/window.fxml");
    windowController.configure(userInfoWindow, userInfoWindowController.getRoot(), true, CLOSE);

    userInfoWindow.show();
  }

  public void onSendPrivateMessage() {
    eventBus.post(new InitiatePrivateChatEvent(player.getUsername()));
  }

  public void onCopyUsername() {
    ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(player.getUsername());
    Clipboard.getSystemClipboard().setContent(clipboardContent);
  }

  public void onAddFriend() {
    if (player.getSocialStatus() == FOE) {
      playerService.removeFoe(player);
    }
    playerService.addFriend(player);
  }

  public void onRemoveFriend() {
    playerService.removeFriend(player);
  }

  public void onAddFoe() {
    if (player.getSocialStatus() == FRIEND) {
      playerService.removeFriend(player);
    }
    playerService.addFoe(player);
  }

  public void onRemoveFoe() {
    playerService.removeFoe(player);
  }

  public void onWatchGame() {
    try {
      replayService.runLiveReplay(player.getGame().getId(), player.getId());
    } catch (Exception e) {
      logger.error("Cannot display live replay {}", e.getCause());
      String title = i18n.get("replays.live.loadFailure.title");
      String message = i18n.get("replays.live.loadFailure.message");
      notificationService.addNotification(new ImmediateNotification(title, message, Severity.ERROR));
    }
  }

  public void onFollowPlayer() {
    playerService.followPlayer(player);
  }

  public void onViewReplays() {
    // FIXME implement
  }

  public void onInviteToGame() {
    //FIXME implement
  }

  public void onKick() {
    // FIXME implement
  }

  public void onBan() {
    // FIXME implement
  }

  public void onJoinGame() {
    joinGameHelper.join(player.getGame());
  }

  public void onRemoveCustomColor() {
    colorPicker.setValue(null);
  }

  @Override
  public ContextMenu getRoot() {
    return chatUserContextMenuRoot;
  }
}
