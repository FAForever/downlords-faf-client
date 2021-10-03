package com.faforever.client.fx;


import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerInfoWindowController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.ClipboardUtil;
import com.faforever.commons.api.dto.GroupPermission;
import com.faforever.commons.lobby.GameType;
import com.google.common.eventbus.EventBus;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.ImageView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.SELF;

@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public abstract class PlayerContextMenuController implements Controller<ContextMenu> {

  private final AvatarService avatarService;
  protected final EventBus eventBus;
  protected final I18n i18n;
  private final JoinGameHelper joinGameHelper;
  protected final ModeratorService moderatorService;
  private final NotificationService notificationService;
  private final PlayerService playerService;
  private final ReplayService replayService;
  protected final UiService uiService;

  public ContextMenu playerContextMenuRoot;
  public MenuItem showUserInfo;
  public MenuItem sendPrivateMessageItem;
  public MenuItem copyUsernameItem;
  public CustomMenuItem colorPickerMenuItem;
  public ColorPicker colorPicker;
  public Button removeCustomColorButton;
  public SeparatorMenuItem socialSeparator;
  public MenuItem inviteItem;
  public MenuItem addFriendItem;
  public MenuItem removeFriendItem;
  public MenuItem addFoeItem;
  public MenuItem removeFoeItem;
  public MenuItem reportItem;
  public SeparatorMenuItem gameSeparator;
  public MenuItem joinGameItem;
  public MenuItem watchGameItem;
  public MenuItem viewReplaysItem;
  public SeparatorMenuItem moderatorActionSeparator;
  public MenuItem kickGameItem;
  public MenuItem kickLobbyItem;
  public MenuItem broadcastMessage;
  public CustomMenuItem avatarPickerMenuItem;
  public ComboBox<AvatarBean> avatarComboBox;

  protected PlayerBean player;
  private InvalidationListener playerPropertyInvalidationListener;

  @Override
  public void initialize() {
    showUserInfo.setOnAction(event -> onShowUserInfoSelected());
    copyUsernameItem.setOnAction(event -> onCopyUsernameSelected());
    addFriendItem.setOnAction(event -> onAddFriendSelected());
    removeFriendItem.setOnAction(event -> onRemoveFriendSelected());
    addFoeItem.setOnAction(event -> onAddFoeSelected());
    removeFoeItem.setOnAction(event -> onRemoveFoeSelected());
    reportItem.setOnAction(event -> onReport());
    joinGameItem.setOnAction(event -> onJoinGameSelected());
    watchGameItem.setOnAction(event -> onWatchGameSelected());
    viewReplaysItem.setOnAction(event -> onViewReplaysSelected());
    kickGameItem.setOnAction(event -> onKickGame());
    kickLobbyItem.setOnAction(event -> onKickLobby());

    avatarComboBox.setCellFactory(param -> avatarCell());
    avatarComboBox.setButtonCell(avatarCell());
    avatarPickerMenuItem.visibleProperty().bind(Bindings.createBooleanBinding(() ->
            !avatarComboBox.getItems().isEmpty() && player.getSocialStatus() == SELF,
        avatarComboBox.getItems()));

    initializeListener();

    copyUsernameItem.setVisible(true);
    socialSeparator.visibleProperty().bind(addFriendItem.visibleProperty()
        .or(removeFriendItem.visibleProperty())
        .or(addFoeItem.visibleProperty())
        .or(removeFoeItem.visibleProperty())
    );
    gameSeparator.visibleProperty().bind(joinGameItem.visibleProperty()
        .or(watchGameItem.visibleProperty())
        .or(viewReplaysItem.visibleProperty())
    );
    moderatorActionSeparator.visibleProperty().bind(kickGameItem.visibleProperty()
        .or(kickLobbyItem.visibleProperty())
        .or(broadcastMessage.visibleProperty())
    );
  }

  @NotNull
  private StringListCell<AvatarBean> avatarCell() {
    return new StringListCell<>(
        AvatarBean::getDescription,
        avatarBean -> new ImageView(avatarService.loadAvatar(avatarBean)));
  }

  protected void initializeListener() {
    playerPropertyInvalidationListener = observable -> {
      setModeratorOptions(moderatorService.getPermissions(), player);
      SocialStatus socialStatus = player.getSocialStatus();
      PlayerStatus playerStatus = player.getStatus();
      GameBean game = player.getGame();
      if (socialStatus == SELF) {
        loadAvailableAvatars(player);
      }
      setItemVisibility(socialStatus, playerStatus, game);
    };
  }

  protected void setItemVisibility(SocialStatus socialStatus, PlayerStatus playerStatus, GameBean game) {
    JavaFxUtil.runLater(() -> {
      showUserInfo.setVisible(true);
      addFriendItem.setVisible(socialStatus != FRIEND && socialStatus != SELF);
      removeFriendItem.setVisible(socialStatus == FRIEND);
      addFoeItem.setVisible(socialStatus != FOE && socialStatus != SELF);
      removeFoeItem.setVisible(socialStatus == FOE);
      reportItem.setVisible(socialStatus != SELF);
      joinGameItem.setVisible(socialStatus != SELF
          && (playerStatus == PlayerStatus.LOBBYING || playerStatus == PlayerStatus.HOSTING)
          && game != null && game.getGameType() != GameType.MATCHMAKER);
      watchGameItem.setVisible(playerStatus == PlayerStatus.PLAYING);
      viewReplaysItem.setVisible(true);
    });
  }

  private void loadAvailableAvatars(PlayerBean player) {
    avatarService.getAvailableAvatars().thenAccept(avatars -> {
      ObservableList<AvatarBean> items = FXCollections.observableArrayList(avatars);
      AvatarBean noAvatar = new AvatarBean();
      noAvatar.setDescription(i18n.get("chat.userContext.noAvatar"));
      items.add(0, noAvatar);

      AvatarBean currentAvatar = player.getAvatar();
      JavaFxUtil.runLater(() -> {
        avatarComboBox.getItems().setAll(items);
        avatarComboBox.getSelectionModel().select(items.stream()
            .filter(avatarBean -> Objects.equals(avatarBean, currentAvatar))
            .findFirst()
            .orElse(null));

        // Only after the box has been populated and we selected the current value, we add the listener.
        // Otherwise the code above already triggers a changeAvatar()
        avatarComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
          player.setAvatar(newValue);
          avatarService.changeAvatar(Objects.requireNonNullElse(newValue, noAvatar));
        });
      });
    });
  }

  public ContextMenu getContextMenu() {
    return getRoot();
  }

  public void setPlayer(PlayerBean player) {
    this.player = player;

    setModeratorOptions(moderatorService.getPermissions(), player);

    WeakInvalidationListener weakPlayerPropertyListener = new WeakInvalidationListener(playerPropertyInvalidationListener);
    JavaFxUtil.addListener(player.gameProperty(), weakPlayerPropertyListener);
    JavaFxUtil.addListener(player.socialStatusProperty(), weakPlayerPropertyListener);
    JavaFxUtil.addAndTriggerListener(player.statusProperty(), weakPlayerPropertyListener);
  }


  public void onShowUserInfoSelected() {
    PlayerInfoWindowController playerInfoWindowController = uiService.loadFxml("theme/user_info_window.fxml");
    playerInfoWindowController.setPlayer(player);
    playerInfoWindowController.setOwnerWindow(getRoot().getOwnerWindow());
    playerInfoWindowController.show();
  }

  public void onCopyUsernameSelected() {
    ClipboardUtil.copyToClipboard(player.getUsername());
  }

  public void onAddFriendSelected() {
    if (player.getSocialStatus() == FOE) {
      playerService.removeFoe(player);
    }
    playerService.addFriend(player);
  }

  public void onRemoveFriendSelected() {
    playerService.removeFriend(player);
  }

  public void onAddFoeSelected() {
    if (player.getSocialStatus() == FRIEND) {
      playerService.removeFriend(player);
    }
    playerService.addFoe(player);
  }

  public void onRemoveFoeSelected() {
    playerService.removeFoe(player);
  }

  public void onReport() {
    ReportDialogController reportDialogController = uiService.loadFxml("theme/reporting/report_dialog.fxml");
    reportDialogController.setOffender(player);
    reportDialogController.setOwnerWindow(getRoot().getOwnerWindow());
    reportDialogController.show();
  }

  public void onJoinGameSelected() {
    joinGameHelper.join(player.getGame());
  }

  public void onWatchGameSelected() {
    try {
      replayService.runLiveReplay(player.getGame().getId());
    } catch (Exception e) {
      log.error("Cannot display live replay", e);
      notificationService.addImmediateErrorNotification(e, "replays.live.loadFailure.message");
    }
  }

  public void onViewReplaysSelected() {
    eventBus.post(new ShowUserReplaysEvent(player.getId()));
  }

  public void onKickGame() {
    moderatorService.closePlayersGame(player);
  }

  public void onKickLobby() {
    moderatorService.closePlayersLobby(player);
  }

  protected void setModeratorOptions(Set<String> permissions, PlayerBean player) {
    boolean notSelf = !player.getSocialStatus().equals(SELF);

    JavaFxUtil.runLater(() -> {
      kickGameItem.setVisible(notSelf & permissions.contains(GroupPermission.ADMIN_KICK_SERVER));
      kickLobbyItem.setVisible(notSelf & permissions.contains(GroupPermission.ADMIN_KICK_SERVER));
    });
  }

  @Override
  public ContextMenu getRoot() {
    return playerContextMenuRoot;
  }

  public void consumer(ActionEvent actionEvent) {
    actionEvent.consume();
  }
}
