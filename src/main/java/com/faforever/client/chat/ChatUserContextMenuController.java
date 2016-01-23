package com.faforever.client.chat;

import com.faforever.client.fx.StageConfigurator;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.GameStatus;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.user.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.chat.SocialStatus.FOE;
import static com.faforever.client.chat.SocialStatus.FRIEND;
import static com.faforever.client.chat.SocialStatus.OTHER;
import static com.faforever.client.chat.SocialStatus.SELF;
import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;

public class ChatUserContextMenuController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @FXML
  MenuItem sendPrivateMessageItem;

  @FXML
  SeparatorMenuItem socialSeperator;

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
  StageConfigurator stageConfigurator;

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

  private PlayerInfoBean playerInfoBean;

  public ContextMenu getContextMenu() {
    return contextMenu;
  }

  public void setPlayerInfoBean(PlayerInfoBean playerInfoBean) {
    this.playerInfoBean = playerInfoBean;
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    if (chatPrefs.getUserToColor().containsKey(playerInfoBean.getUsername())) {
      colorPicker.setValue(chatPrefs.getUserToColor().get(playerInfoBean.getUsername()));
    } else {
      colorPicker.setValue(null);
    }

    colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        chatPrefs.getUserToColor().remove(playerInfoBean.getUsername());
      } else {
        chatPrefs.getUserToColor().put(playerInfoBean.getUsername(), newValue);
      }
      ChatUser chatUser = chatService.createOrGetChatUser(playerInfoBean.getUsername());
      chatUser.setColor(newValue);
      contextMenu.hide();
    });

    removeCustomColorItem.visibleProperty().bind(chatPrefs.chatColorModeProperty().isEqualTo(CUSTOM)
        .and(colorPicker.valueProperty().isNotNull())
        .and(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF)));
    colorPickerMenuItem.visibleProperty().bind(chatPrefs.chatColorModeProperty()
        .isEqualTo(CUSTOM)
        .and(playerInfoBean.socialStatusProperty().isEqualTo(OTHER)));

    sendPrivateMessageItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF));

    addFriendItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isNotEqualTo(FRIEND).and(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF)));
    removeFriendItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isEqualTo(FRIEND));
    addFoeItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isNotEqualTo(FOE).and(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF)));
    removeFoeItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isEqualTo(FOE));

    joinGameItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF)
        .and(playerInfoBean.gameStatusProperty().isEqualTo(GameStatus.LOBBY)
            .or(playerInfoBean.gameStatusProperty().isEqualTo(GameStatus.HOST))));
    watchGameItem.visibleProperty().bind(playerInfoBean.gameStatusProperty().isEqualTo(GameStatus.PLAYING));
    inviteItem.visibleProperty().bind(playerInfoBean.socialStatusProperty().isNotEqualTo(SELF)
        .and(playerInfoBean.gameStatusProperty().isNotEqualTo(GameStatus.PLAYING)));

    socialSeperator.visibleProperty().bind(addFriendItem.visibleProperty().or(
        removeFriendItem.visibleProperty().or(
            addFoeItem.visibleProperty().or(
                removeFoeItem.visibleProperty()))));
  }

  @FXML
  void onUserInfo() {
    UserInfoWindowController userInfoWindowController = applicationContext.getBean(UserInfoWindowController.class);
    userInfoWindowController.setPlayerInfoBean(playerInfoBean);

    Stage userInfoWindow = new Stage(StageStyle.TRANSPARENT);
    userInfoWindow.initModality(Modality.NONE);
    userInfoWindow.initOwner(contextMenu.getOwnerWindow());

    stageConfigurator.configureScene(userInfoWindow, userInfoWindowController.getRoot(), true, CLOSE);

    userInfoWindow.show();
  }

  @FXML
  void onSendPrivateMessage() {
  }

  @FXML
  void onAddFriend() {
    if (playerInfoBean.getSocialStatus() == FOE) {
      playerService.removeFoe(playerInfoBean.getUsername());
    }
    playerService.addFriend(playerInfoBean.getUsername());
  }

  @FXML
  void onRemoveFriend() {
    playerService.removeFriend(playerInfoBean.getUsername());
  }

  @FXML
  void onAddFoe() {
    if (playerInfoBean.getSocialStatus() == FRIEND) {
      playerService.removeFriend(playerInfoBean.getUsername());
    }
    playerService.addFoe(playerInfoBean.getUsername());
  }

  @FXML
  void onRemoveFoe() {
    playerService.removeFoe(playerInfoBean.getUsername());
  }

  @FXML
  void onWatchGame() {
    try {
      replayService.runLiveReplay(playerInfoBean.getGameUid(), playerInfoBean.getUsername());
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
    gameService.joinGame(gameInfoBean, null)
        .exceptionally(throwable -> {
          // FIXME implement
          logger.warn("Game could not be joined", throwable);
          return null;
        });
  }

  @FXML
  void onRemoveCustomColor() {
    colorPicker.setValue(null);
  }
}
