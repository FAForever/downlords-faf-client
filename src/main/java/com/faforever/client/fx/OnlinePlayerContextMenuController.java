package com.faforever.client.fx;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.theme.UiService;
import com.faforever.commons.api.dto.GroupPermission;
import com.faforever.commons.lobby.GameType;
import com.google.common.eventbus.EventBus;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.ImageView;
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
public abstract class OnlinePlayerContextMenuController extends AbstractPlayerContextMenuController {

  private InvalidationListener playerPropertyInvalidationListener;

  public OnlinePlayerContextMenuController(AvatarService avatarService,
                                          EventBus eventBus,
                                          I18n i18n,
                                          JoinGameHelper joinGameHelper,
                                          ModeratorService moderatorService,
                                          NotificationService notificationService,
                                          PlayerService playerService,
                                          ReplayService replayService,
                                          UiService uiService) {
    super(avatarService, eventBus, i18n, joinGameHelper, moderatorService, notificationService, playerService, replayService, uiService);
  }

  @Override
  public void initialize() {
    super.initialize();
    sendPrivateMessageItem.setOnAction(event -> onSendPrivateMessageSelected());
    addFriendItem.setOnAction(event -> onAddFriendSelected());
    removeFriendItem.setOnAction(event -> onRemoveFriendSelected());
    addFoeItem.setOnAction(event -> onAddFoeSelected());
    removeFoeItem.setOnAction(event -> onRemoveFoeSelected());
    reportItem.setOnAction(event -> onReport());
    joinGameItem.setOnAction(event -> onJoinGameSelected());
    watchGameItem.setOnAction(event -> onWatchGameSelected());
    kickGameItem.setOnAction(event -> onKickGame());
    kickLobbyItem.setOnAction(event -> onKickLobby());

    avatarComboBox.setCellFactory(param -> avatarCell());
    avatarComboBox.setButtonCell(avatarCell());

    initializeListener();

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
      PlayerBean player = getPlayer();
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
      sendPrivateMessageItem.setVisible(socialStatus != SELF);
      addFriendItem.setVisible(socialStatus != FRIEND && socialStatus != SELF);
      removeFriendItem.setVisible(socialStatus == FRIEND);
      addFoeItem.setVisible(socialStatus != FOE && socialStatus != SELF);
      removeFoeItem.setVisible(socialStatus == FOE);
      reportItem.setVisible(socialStatus != SELF);
      joinGameItem.setVisible(socialStatus != SELF
          && (playerStatus == PlayerStatus.LOBBYING || playerStatus == PlayerStatus.HOSTING)
          && game != null && game.getGameType() != GameType.MATCHMAKER);
      watchGameItem.setVisible(playerStatus == PlayerStatus.PLAYING);
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

        // Only after the box has been populated, and we selected the current value, we add the listener.
        // Otherwise, the code above already triggers a changeAvatar()
        avatarComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
          player.setAvatar(newValue);
          avatarService.changeAvatar(Objects.requireNonNullElse(newValue, noAvatar));
        });
        avatarPickerMenuItem.setVisible(!avatarComboBox.getItems().isEmpty());
      });
    });
  }

  @Override
  public void setPlayer(PlayerBean player) {
    super.setPlayer(player);

    setModeratorOptions(moderatorService.getPermissions(), player);

    WeakInvalidationListener weakPlayerPropertyListener = new WeakInvalidationListener(playerPropertyInvalidationListener);
    JavaFxUtil.addListener(player.gameProperty(), weakPlayerPropertyListener);
    JavaFxUtil.addListener(player.socialStatusProperty(), weakPlayerPropertyListener);
    JavaFxUtil.addAndTriggerListener(player.statusProperty(), weakPlayerPropertyListener);
  }

  public void onSendPrivateMessageSelected() {
    eventBus.post(new InitiatePrivateChatEvent(getPlayer().getUsername()));
  }

  public void onAddFriendSelected() {
    PlayerBean player = getPlayer();
    if (player.getSocialStatus() == FOE) {
      playerService.removeFoe(player);
    }
    playerService.addFriend(player);
  }

  public void onRemoveFriendSelected() {
    PlayerBean player = getPlayer();
    playerService.removeFriend(player);
  }

  public void onAddFoeSelected() {
    PlayerBean player = getPlayer();
    if (player.getSocialStatus() == FRIEND) {
      playerService.removeFriend(player);
    }
    playerService.addFoe(player);
  }

  public void onRemoveFoeSelected() {
    PlayerBean player = getPlayer();
    playerService.removeFoe(player);
  }

  public void onReport() {
    ReportDialogController reportDialogController = uiService.loadFxml("theme/reporting/report_dialog.fxml");
    PlayerBean player = getPlayer();
    reportDialogController.setOffender(player);
    reportDialogController.setOwnerWindow(getRoot().getOwnerWindow());
    reportDialogController.show();
  }

  public void onJoinGameSelected() {
    PlayerBean player = getPlayer();
    joinGameHelper.join(player.getGame());
  }

  public void onWatchGameSelected() {
    PlayerBean player = getPlayer();
    try {
      replayService.runLiveReplay(player.getGame().getId());
    } catch (Exception e) {
      log.error("Cannot display live replay", e);
      notificationService.addImmediateErrorNotification(e, "replays.live.loadFailure.message");
    }
  }

  public void onKickGame() {
    moderatorService.closePlayersGame(getPlayer());
  }

  public void onKickLobby() {
    moderatorService.closePlayersLobby(getPlayer());
  }

  protected void setModeratorOptions(Set<String> permissions, PlayerBean player) {
    boolean notSelf = !player.getSocialStatus().equals(SELF);

    JavaFxUtil.runLater(() -> {
      kickGameItem.setVisible(notSelf & permissions.contains(GroupPermission.ADMIN_KICK_SERVER));
      kickLobbyItem.setVisible(notSelf & permissions.contains(GroupPermission.ADMIN_KICK_SERVER));
    });
  }
}
