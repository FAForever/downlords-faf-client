package com.faforever.client.chat;

import com.faforever.client.api.dto.GroupPermission;
import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.faforever.client.moderator.BanDialogController;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import com.jfoenix.animation.alert.JFXAlertAnimation;
import com.jfoenix.controls.JFXAlert;
import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Objects;
import java.util.Set;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.SELF;
import static java.util.Locale.US;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class ChatUserContextMenuController implements Controller<ContextMenu> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final PreferencesService preferencesService;
  private final PlayerService playerService;
  private final ReplayService replayService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final EventBus eventBus;
  private final JoinGameHelper joinGameHelper;
  private final AvatarService avatarService;
  private final UiService uiService;
  private final ModeratorService moderatorService;
  public ComboBox<AvatarBean> avatarComboBox;
  public CustomMenuItem avatarPickerMenuItem;
  public MenuItem sendPrivateMessageItem;
  public SeparatorMenuItem socialSeparator;
  public CustomMenuItem colorPickerMenuItem;
  public ColorPicker colorPicker;
  public MenuItem joinGameItem;
  public MenuItem addFriendItem;
  public MenuItem removeFriendItem;
  public MenuItem addFoeItem;
  public MenuItem removeFoeItem;
  public MenuItem watchGameItem;
  public MenuItem viewReplaysItem;
  public MenuItem inviteItem;
  public SeparatorMenuItem moderatorActionSeparator;
  public MenuItem banItem;
  public ContextMenu chatUserContextMenuRoot;
  public MenuItem showUserInfo;
  public JFXButton removeCustomColorButton;
  private ChatChannelUser chatUser;
  public MenuItem kickGameItem;
  public MenuItem kickLobbyItem;

  @SuppressWarnings("FieldCanBeLocal")
  private ChangeListener<Player> playerChangeListener;

  public ChatUserContextMenuController(PreferencesService preferencesService,
                                       PlayerService playerService, ReplayService replayService,
                                       NotificationService notificationService, I18n i18n, EventBus eventBus,
                                       JoinGameHelper joinGameHelper, AvatarService avatarService, UiService uiService, ModeratorService moderatorService) {
    this.preferencesService = preferencesService;
    this.playerService = playerService;
    this.replayService = replayService;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.eventBus = eventBus;
    this.joinGameHelper = joinGameHelper;
    this.avatarService = avatarService;
    this.uiService = uiService;
    this.moderatorService = moderatorService;
  }

  public void initialize() {
    avatarComboBox.setCellFactory(param -> avatarCell());
    avatarComboBox.setButtonCell(avatarCell());
    removeCustomColorButton.managedProperty().bind(removeCustomColorButton.visibleProperty());

    avatarPickerMenuItem.visibleProperty().bind(Bindings.createBooleanBinding(() -> !avatarComboBox.getItems().isEmpty(), avatarComboBox.getItems()));

    // Workaround for the issue that the popup gets closed when the "custom color" button is clicked, causing an NPE
    // in the custom color popup window.
    colorPicker.focusedProperty().addListener((observable, oldValue, newValue) -> chatUserContextMenuRoot.setAutoHide(!newValue));
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

  public void setChatUser(ChatChannelUser chatUser) {
    this.chatUser = chatUser;
    showUserInfo.visibleProperty().bind(chatUser.playerProperty().isNotNull());
    viewReplaysItem.visibleProperty().bind(chatUser.playerProperty().isNotNull());

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    String lowerCaseUsername = chatUser.getUsername().toLowerCase(US);
    colorPicker.setValue(chatPrefs.getUserToColor().getOrDefault(lowerCaseUsername, null));

    colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
      String lowerUsername = chatUser.getUsername().toLowerCase(US);
      if (newValue == null) {
        chatPrefs.getUserToColor().remove(lowerUsername);
      } else {
        chatPrefs.getUserToColor().put(lowerUsername, newValue);
      }
      chatUser.setColor(newValue);
      chatUserContextMenuRoot.hide();
    });

    removeCustomColorButton.visibleProperty().bind(chatPrefs.chatColorModeProperty().isEqualTo(CUSTOM)
        .and(colorPicker.valueProperty().isNotNull()));
    colorPickerMenuItem.visibleProperty().bind(chatPrefs.chatColorModeProperty()
        .isEqualTo(CUSTOM));


    playerChangeListener = (observable, oldValue, newValue) -> {
      if (newValue == null) {
        return;
      }

      if (newValue.getSocialStatus() == SELF) {
        loadAvailableAvatars(newValue);
      }

      moderatorService.getPermissions()
          .thenAccept(permissions -> setModeratorOptions(permissions, newValue));

      sendPrivateMessageItem.visibleProperty().bind(newValue.socialStatusProperty().isNotEqualTo(SELF));
      addFriendItem.visibleProperty().bind(
          newValue.socialStatusProperty().isNotEqualTo(FRIEND).and(newValue.socialStatusProperty().isNotEqualTo(SELF))
      );
      removeFriendItem.visibleProperty().bind(newValue.socialStatusProperty().isEqualTo(FRIEND));
      addFoeItem.visibleProperty().bind(newValue.socialStatusProperty().isNotEqualTo(FOE).and(newValue.socialStatusProperty().isNotEqualTo(SELF)));
      removeFoeItem.visibleProperty().bind(newValue.socialStatusProperty().isEqualTo(FOE));

      // TODO: Make this ignore TMM games too and not just ladder
      // https://github.com/FAForever/downlords-faf-client/issues/1770
      joinGameItem.visibleProperty().bind(newValue.socialStatusProperty().isNotEqualTo(SELF)
          .and(newValue.statusProperty().isEqualTo(PlayerStatus.LOBBYING)
              .or(newValue.statusProperty().isEqualTo(PlayerStatus.HOSTING)))
          .and(Bindings.createBooleanBinding(() -> {
                return newValue.getGame() != null
                    && newValue.getGame().getFeaturedMod() != null
                    && !newValue.getGame().getFeaturedMod().equals(KnownFeaturedMod.LADDER_1V1.getTechnicalName());
              }, newValue.gameProperty())
          ));
      watchGameItem.visibleProperty().bind(newValue.statusProperty().isEqualTo(PlayerStatus.PLAYING));
      inviteItem.visibleProperty().bind(newValue.socialStatusProperty().isNotEqualTo(SELF)
          .and(newValue.statusProperty().isNotEqualTo(PlayerStatus.PLAYING)));

    };
    JavaFxUtil.addListener(chatUser.playerProperty(), new WeakChangeListener<>(playerChangeListener));
    playerChangeListener.changed(chatUser.playerProperty(), null, chatUser.getPlayer().orElse(null));

    socialSeparator.visibleProperty().bind(addFriendItem.visibleProperty().or(
        removeFriendItem.visibleProperty().or(
            addFoeItem.visibleProperty().or(
                removeFoeItem.visibleProperty()))));
  }

  private void setModeratorOptions(Set<String> permissions, Player newValue) {
    boolean notSelf = !newValue.getSocialStatus().equals(SELF);

    kickGameItem.setVisible(notSelf & permissions.contains(GroupPermission.ADMIN_KICK_SERVER));
    kickLobbyItem.setVisible(notSelf & permissions.contains(GroupPermission.ADMIN_KICK_SERVER));
    banItem.setVisible(notSelf & permissions.contains(GroupPermission.ROLE_ADMIN_ACCOUNT_BAN));
    moderatorActionSeparator.setVisible(kickGameItem.isVisible() || kickLobbyItem.isVisible() || banItem.isVisible());

  }

  private void loadAvailableAvatars(Player player) {
    avatarService.getAvailableAvatars().thenAccept(avatars -> {
      ObservableList<AvatarBean> items = FXCollections.observableArrayList(avatars);
      items.add(0, new AvatarBean(null, i18n.get("chat.userContext.noAvatar")));

      String currentAvatarUrl = player.getAvatarUrl();
      Platform.runLater(() -> {
        avatarComboBox.getItems().setAll(items);
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

  public void onShowUserInfoSelected() {
    UserInfoWindowController userInfoWindowController = uiService.loadFxml("theme/user_info_window.fxml");
    userInfoWindowController.setPlayer(chatUser.getPlayer().orElseThrow(() -> new IllegalStateException("No player for chat user: " + chatUser)));
    userInfoWindowController.setOwnerWindow(chatUserContextMenuRoot.getOwnerWindow());
    userInfoWindowController.show();
  }

  public void onSendPrivateMessageSelected() {
    eventBus.post(new InitiatePrivateChatEvent(chatUser.getUsername()));
  }

  public void onCopyUsernameSelected() {
    ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(chatUser.getUsername());
    Clipboard.getSystemClipboard().setContent(clipboardContent);
  }

  public void onAddFriendSelected() {
    Player player = getPlayer();
    if (player.getSocialStatus() == FOE) {
      playerService.removeFoe(player);
    }
    playerService.addFriend(player);
  }

  public void onRemoveFriendSelected() {
    Player player = getPlayer();
    playerService.removeFriend(player);
  }

  public void onAddFoeSelected() {
    Player player = getPlayer();
    if (player.getSocialStatus() == FRIEND) {
      playerService.removeFriend(player);
    }
    playerService.addFoe(player);
  }

  public void onRemoveFoeSelected() {
    Player player = getPlayer();
    playerService.removeFoe(player);
  }

  public void onWatchGameSelected() {
    Player player = getPlayer();
    try {
      replayService.runLiveReplay(player.getGame().getId());
    } catch (Exception e) {
      logger.error("Cannot display live replay {}", e.getCause());
      String title = i18n.get("replays.live.loadFailure.title");
      String message = i18n.get("replays.live.loadFailure.message");
      notificationService.addNotification(new ImmediateNotification(title, message, Severity.ERROR));
    }
  }

  public void onViewReplaysSelected() {
    Player player = getPlayer();
    eventBus.post(new ShowUserReplaysEvent(player.getId()));
  }

  public void onInviteToGameSelected() {
    //FIXME implement
  }

  public void onBan(ActionEvent actionEvent) {
    actionEvent.consume();
    JFXAlert<?> dialog = new JFXAlert<>((Stage) getRoot().getOwnerWindow());

    BanDialogController controller = ((BanDialogController) uiService.loadFxml("theme/moderator/ban_dialog.fxml"))
        .setPlayer(getPlayer())
        .setCloseListener(dialog::close);

    dialog.setContent(controller.getDialogLayout());
    dialog.setAnimation(JFXAlertAnimation.TOP_ANIMATION);
    dialog.show();
  }

  public void onJoinGameSelected() {
    Player player = getPlayer();
    joinGameHelper.join(player.getGame());
  }

  @NotNull
  private Player getPlayer() {
    return chatUser.getPlayer().orElseThrow(() -> new IllegalStateException("No player for chat user:" + chatUser));
  }

  public void onRemoveCustomColor() {
    colorPicker.setValue(null);
  }

  @Override
  public ContextMenu getRoot() {
    return chatUserContextMenuRoot;
  }

  public void onKickGame() {
    moderatorService.closePlayersGame(getPlayer().getId());
  }

  public void onKickLobby() {
    moderatorService.closePlayersLobby(getPlayer().getId());
  }


  public void consumer(ActionEvent actionEvent) {
    actionEvent.consume();
  }
}
