package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.fx.WindowController;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GameStatus;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Objects;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.chat.SocialStatus.FOE;
import static com.faforever.client.chat.SocialStatus.FRIEND;
import static com.faforever.client.chat.SocialStatus.SELF;
import static com.faforever.client.fx.WindowController.WindowButtonType.CLOSE;
import static java.util.Locale.US;

public class ChatUserContextMenuController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @FXML
  ComboBox<AvatarBean> avatarComboBox;
  @FXML
  CustomMenuItem avatarPickerMenuItem;
  @FXML
  MenuItem sendPrivateMessageItem;
  @FXML
  SeparatorMenuItem socialSeparator;
  @FXML
  MenuItem removeCustomColorItem;
  @FXML
  CustomMenuItem colorPickerMenuItem;
  @FXML
  ColorPicker colorPicker;
  @FXML
  MenuItem joinGameItem;
  @FXML
  MenuItem addFriendItem;
  @FXML
  MenuItem removeFriendItem;
  @FXML
  MenuItem addFoeItem;
  @FXML
  MenuItem removeFoeItem;
  @FXML
  MenuItem watchGameItem;
  @FXML
  MenuItem viewReplaysItem;
  @FXML
  MenuItem inviteItem;
  @FXML
  SeparatorMenuItem moderatorActionSeparator;
  @FXML
  MenuItem kickItem;
  @FXML
  MenuItem banItem;
  @FXML
  ContextMenu contextMenu;

  @Resource
  UserService userService;
  @Resource
  ChatService chatService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  PlayerService playerService;
  @Resource
  GameService gameService;
  @Resource
  ReplayService replayService;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  EventBus eventBus;
  @Resource
  JoinGameHelper joinGameHelper;
  @Resource
  AvatarService avatarService;

  private PlayerInfoBean playerInfoBean;

  @FXML
  void initialize() {
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
          return avatarService.loadAvatar(url.toString());
        });
  }

  public ContextMenu getContextMenu() {
    return contextMenu;
  }

  public void setPlayerInfoBean(PlayerInfoBean playerInfoBean) {
    this.playerInfoBean = playerInfoBean;
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    String lowerCaseUsername = playerInfoBean.getUsername().toLowerCase(US);
    if (chatPrefs.getUserToColor().containsKey(lowerCaseUsername)) {
      colorPicker.setValue(chatPrefs.getUserToColor().get(lowerCaseUsername));
    } else {
      colorPicker.setValue(null);
    }

    colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
      String lowerUsername = playerInfoBean.getUsername().toLowerCase(US);
      if (newValue == null) {
        chatPrefs.getUserToColor().remove(lowerUsername);
      } else {
        chatPrefs.getUserToColor().put(lowerUsername, newValue);
      }
      ChatUser chatUser = chatService.getOrCreateChatUser(lowerUsername);
      chatUser.setColor(newValue);
      contextMenu.hide();
    });

    removeCustomColorItem.visibleProperty().bind(chatPrefs.chatColorModeProperty().isEqualTo(CUSTOM)
        .and(colorPicker.valueProperty().isNotNull())
        .and(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF)));
    colorPickerMenuItem.visibleProperty().bind(chatPrefs.chatColorModeProperty()
        .isEqualTo(CUSTOM)
        .and(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF)));

    if (playerInfoBean.getSocialStatus() != SocialStatus.SELF) {
      avatarPickerMenuItem.setVisible(false);
    } else {
      loadAvailableAvatars();
    }

    kickItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF));
    banItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF));
    moderatorActionSeparator.visibleProperty().bind(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF));

    sendPrivateMessageItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF));

    addFriendItem.visibleProperty().bind(
        playerInfoBean.socialStatusProperty().isNotEqualTo(FRIEND).and(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF))
    );
    removeFriendItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isEqualTo(FRIEND));
    addFoeItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isNotEqualTo(FOE).and(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF)));
    removeFoeItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isEqualTo(FOE));

    joinGameItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF)
        .and(playerInfoBean.gameStatusProperty().isEqualTo(GameStatus.LOBBY)
            .or(playerInfoBean.gameStatusProperty().isEqualTo(GameStatus.HOST))));
    watchGameItem.visibleProperty().bind(playerInfoBean.gameStatusProperty().isEqualTo(GameStatus.PLAYING));
    inviteItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF)
        .and(playerInfoBean.gameStatusProperty().isNotEqualTo(GameStatus.PLAYING)));

    socialSeparator.visibleProperty().bind(addFriendItem.visibleProperty().or(
        removeFriendItem.visibleProperty().or(
            addFoeItem.visibleProperty().or(
                removeFoeItem.visibleProperty()))));
  }

  private void loadAvailableAvatars() {
    avatarService.getAvailableAvatars().thenAccept(avatars -> {
      ObservableList<AvatarBean> items = FXCollections.observableArrayList(avatars);
      items.add(0, new AvatarBean(null, i18n.get("chat.userContext.noAvatar")));


      String currentAvatarUrl = playerInfoBean.getAvatarUrl();
      Platform.runLater(() -> {
        avatarComboBox.setItems(items);
        avatarComboBox.getSelectionModel().select(items.stream()
            .filter(avatarBean -> Objects.equals(Objects.toString(avatarBean.getUrl(), null), currentAvatarUrl))
            .findFirst()
            .orElse(null));

        // Only after the box has been populated and we selected the current value, we add the listener.
        // Otherwise the code above already triggers a changeAvatar()
        avatarComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
          playerInfoBean.setAvatarTooltip(newValue == null ? null : newValue.getDescription());
          playerInfoBean.setAvatarUrl(newValue == null ? null : Objects.toString(newValue.getUrl(), null));
          avatarService.changeAvatar(newValue);
        });
      });

    });
  }

  @FXML
  void onUserInfo() {
    UserInfoWindowController userInfoWindowController = applicationContext.getBean(UserInfoWindowController.class);
    userInfoWindowController.setPlayerInfoBean(playerInfoBean);

    Stage userInfoWindow = new Stage(StageStyle.TRANSPARENT);
    userInfoWindow.initModality(Modality.NONE);
    userInfoWindow.initOwner(contextMenu.getOwnerWindow());

    WindowController windowController = applicationContext.getBean(WindowController.class);
    windowController.configure(userInfoWindow, userInfoWindowController.getRoot(), true, CLOSE);

    userInfoWindow.show();
  }

  @FXML
  void onSendPrivateMessage() {
    eventBus.post(new InitiatePrivateChatEvent(playerInfoBean.getUsername()));
  }

  @FXML
  void onAddFriend() {
    if (playerInfoBean.getSocialStatus() == FOE) {
      playerService.removeFoe(playerInfoBean);
    }
    playerService.addFriend(playerInfoBean);
  }

  @FXML
  void onRemoveFriend() {
    playerService.removeFriend(playerInfoBean);
  }

  @FXML
  void onAddFoe() {
    if (playerInfoBean.getSocialStatus() == FRIEND) {
      playerService.removeFriend(playerInfoBean);
    }
    playerService.addFoe(playerInfoBean);
  }

  @FXML
  void onRemoveFoe() {
    playerService.removeFoe(playerInfoBean);
  }

  @FXML
  void onWatchGame() {
    try {
      replayService.runLiveReplay(playerInfoBean.getGameUid(), playerInfoBean.getId());
    } catch (IOException e) {
      logger.error("Cannot load live replay {}", e.getCause());
      String title = i18n.get("replays.live.loadFailure.title");
      String message = i18n.get("replays.live.loadFailure.message");
      notificationService.addNotification(new ImmediateNotification(title, message, Severity.ERROR));
    }
  }

  @FXML
  void onViewReplays() {
    // FIXME implement
  }

  @FXML
  void onInviteToGame() {
    //FIXME implement
  }

  @FXML
  void onKick() {
    // FIXME implement
  }

  @FXML
  void onBan() {
    // FIXME implement
  }

  @FXML
  void onJoinGame() {
    GameInfoBean gameInfoBean = gameService.getByUid(playerInfoBean.getGameUid());
    joinGameHelper.join(gameInfoBean);
  }

  @FXML
  void onRemoveCustomColor() {
    colorPicker.setValue(null);
  }
}
