package com.faforever.client.user;

import com.faforever.client.avatar.AvatarBean;
import com.faforever.client.avatar.AvatarService;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.chat.event.ChatUserColorChangeEvent;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.StringListCell;
import com.faforever.client.game.Game;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerInfoWindowController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.teammatchmaking.TeamMatchmakingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Assert;
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
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.faforever.client.chat.ChatColorMode.RANDOM;
import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.SELF;
import static java.util.Locale.US;

@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class UserContextMenuController implements Controller<ContextMenu> {

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
  private final TeamMatchmakingService teamMatchmakingService;

  public ContextMenu contextMenuRoot;

  public MenuItem showPlayerInfoItem;
  public MenuItem sendPrivateMessageItem;
  public MenuItem copyPLayerNameItem;

  public SeparatorMenuItem socialSeparator;
  public MenuItem invitePlayerItem;
  public MenuItem addFriendItem;
  public MenuItem removeFriendItem;
  public MenuItem addFoeItem;
  public MenuItem removeFoeItem;
  public MenuItem reportPlayerItem;

  public SeparatorMenuItem otherSeparator;
  public MenuItem joinGameItem;
  public MenuItem watchGameItem;
  public MenuItem viewReplaysItem;

  public SeparatorMenuItem moderatorActionSeparator;
  public MenuItem kickGameItem;
  public MenuItem kickLobbyItem;
  public MenuItem broadcastMessage;

  public SeparatorMenuItem chatChannelSeparator;
  public CustomMenuItem colorPickerMenuItem;
  public ColorPicker colorPicker;
  public Button removeCustomColorButton;
  public CustomMenuItem avatarPickerMenuItem;
  public ComboBox<AvatarBean> avatarComboBox;

  private ChatChannelUser chatUser;
  private InvalidationListener chatUserPropertyInvalidationListener;
  private Player player;

  @Override
  public void initialize() {
    JavaFxUtil.bind(socialSeparator.visibleProperty(), invitePlayerItem.visibleProperty().or(addFriendItem.visibleProperty()
        .or(removeFriendItem.visibleProperty().or(addFoeItem.visibleProperty().or(removeFoeItem.visibleProperty()
            .or(reportPlayerItem.visibleProperty()))))));
    JavaFxUtil.bind(otherSeparator.visibleProperty(), joinGameItem.visibleProperty().or(watchGameItem.visibleProperty()
        .or(viewReplaysItem.visibleProperty())));
    JavaFxUtil.bind(moderatorActionSeparator.visibleProperty(), kickGameItem.visibleProperty().or(kickLobbyItem.visibleProperty()
        .or(broadcastMessage.visibleProperty())));
    JavaFxUtil.bind(chatChannelSeparator.visibleProperty(), colorPickerMenuItem.visibleProperty()
        .or(avatarPickerMenuItem.visibleProperty()));
    JavaFxUtil.bindManagedToVisible(removeCustomColorButton);
    avatarComboBox.setCellFactory(param -> avatarCell());
    avatarComboBox.setButtonCell(avatarCell());
    JavaFxUtil.bind(avatarPickerMenuItem.visibleProperty(), Bindings.createBooleanBinding(() ->
        !avatarComboBox.getItems().isEmpty(), avatarComboBox.getItems()));
  }

  private void initializeListenersForChatUser() {
    chatUserPropertyInvalidationListener = observable -> {
      Optional<Player> optionalPlayer = chatUser.getPlayer();
      optionalPlayer.ifPresent(player ->
          moderatorService.getPermissions().thenAccept(permissions -> setModeratorOptions(permissions, player)));
      SocialStatus socialStatus = chatUser.getSocialStatus().orElse(null);
      PlayerStatus playerStatus = chatUser.getGameStatus().orElse(null);
      Game game = optionalPlayer.map(Player::getGame).orElse(null);
      if (socialStatus == SELF && optionalPlayer.isPresent()) {
        loadAvailableAvatars(optionalPlayer.get());
      }
      JavaFxUtil.runLater(() -> {
        showPlayerInfoItem.setVisible(optionalPlayer.isPresent());
        viewReplaysItem.setVisible(optionalPlayer.isPresent());
        sendPrivateMessageItem.setVisible(socialStatus != SELF);
        setSocialItemsVisibility(socialStatus);

        joinGameItem.setVisible(socialStatus != SELF
            && (playerStatus == PlayerStatus.LOBBYING || playerStatus == PlayerStatus.HOSTING)
            && game != null && game.getGameType() != GameType.MATCHMAKER);
        watchGameItem.setVisible(playerStatus == PlayerStatus.PLAYING);
        invitePlayerItem.setVisible(socialStatus != SELF && playerStatus == PlayerStatus.IDLE);
      });
    };
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

  public void setChatUser(ChatChannelUser chatUser) {
    Assert.checkNotNullIllegalState(this.chatUser, "Chat User already set");
    Assert.checkNotNullIllegalState(this.player, "Player already set");
    this.chatUser = chatUser;
    initializeListenersForChatUser();

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    String lowerCaseUsername = chatUser.getUsername().toLowerCase(US);
    colorPicker.setValue(chatPrefs.getUserToColor().getOrDefault(lowerCaseUsername, null));

    colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
      String lowerUsername = chatUser.getUsername().toLowerCase(US);
      ChatUserCategory userCategory;
      if (chatUser.isModerator()) {
        userCategory = ChatUserCategory.MODERATOR;
      } else {
        userCategory = chatUser.getSocialStatus().map(status -> switch (status) {
          case FRIEND -> ChatUserCategory.FRIEND;
          case FOE -> ChatUserCategory.FOE;
          default -> ChatUserCategory.OTHER;
        }).orElse(ChatUserCategory.OTHER);
      }
      if (newValue == null) {
        chatPrefs.getUserToColor().remove(lowerUsername);
        chatUser.setColor(chatPrefs.getGroupToColor().getOrDefault(userCategory, null));
      } else {
        chatPrefs.getUserToColor().put(lowerUsername, newValue);
        chatUser.setColor(newValue);
      }
      eventBus.post(new ChatUserColorChangeEvent(chatUser));
    });

    removeCustomColorButton.visibleProperty().bind(chatPrefs.chatColorModeProperty().isNotEqualTo(RANDOM)
        .and(colorPicker.valueProperty().isNotNull()));
    colorPickerMenuItem.visibleProperty().bind(chatPrefs.chatColorModeProperty().isNotEqualTo(RANDOM));

    WeakInvalidationListener weakChatUserPropertyListener = new WeakInvalidationListener(chatUserPropertyInvalidationListener);
    JavaFxUtil.addListener(chatUser.playerProperty(), weakChatUserPropertyListener);
    JavaFxUtil.addListener(chatUser.socialStatusProperty(), weakChatUserPropertyListener);
    JavaFxUtil.addAndTriggerListener(chatUser.gameStatusProperty(), weakChatUserPropertyListener);
  }

  public void setPlayer(Player player) {
    Assert.checkNotNullIllegalState(this.player, "Player already set");
    Assert.checkNotNullIllegalState(this.chatUser, "Chat User already set");
    this.player = player;
  }

  public void showSocialItems() {
    setSocialItemsVisibility(getPlayer().getSocialStatus());
  }

  private void setSocialItemsVisibility(SocialStatus status) {
    JavaFxUtil.runLater(() -> {
      addFriendItem.setVisible(status != FRIEND && status != SELF);
      removeFriendItem.setVisible(status == FRIEND);
      addFoeItem.setVisible(status != FOE && status != SELF);
      removeFoeItem.setVisible(status == FOE);
      reportPlayerItem.setVisible(status != SELF);
    });
  }

  private void setModeratorOptions(Set<String> permissions, Player player) {
    boolean notSelf = !player.getSocialStatus().equals(SELF);

    JavaFxUtil.runLater(() -> {
      kickGameItem.setVisible(notSelf & permissions.contains(GroupPermission.ADMIN_KICK_SERVER));
      kickLobbyItem.setVisible(notSelf & permissions.contains(GroupPermission.ADMIN_KICK_SERVER));
      broadcastMessage.setVisible(notSelf & permissions.contains(GroupPermission.ROLE_WRITE_MESSAGE));
    });
  }

  private void loadAvailableAvatars(Player player) {
    avatarService.getAvailableAvatars().thenAccept(avatars -> {
      ObservableList<AvatarBean> items = FXCollections.observableArrayList(avatars);
      items.add(0, new AvatarBean(null, i18n.get("chat.userContext.noAvatar")));

      String currentAvatarUrl = player.getAvatarUrl();
      JavaFxUtil.runLater(() -> {
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
    PlayerInfoWindowController playerInfoWindowController = uiService.loadFxml("theme/user_info_window.fxml");
    playerInfoWindowController.setPlayer(getPlayer());
    playerInfoWindowController.setOwnerWindow(contextMenuRoot.getOwnerWindow());
    playerInfoWindowController.show();
  }

  public void onSendPrivateMessageSelected() {
    eventBus.post(new InitiatePrivateChatEvent(getUsername()));
  }

  public void onCopyPlayerNameSelected() {
    ClipboardUtil.copyToClipboard(getUsername());
  }

  public void onAddFriendSelected() {
    Player player = getPlayer();
    if (player.getSocialStatus() == FOE) {
      playerService.removeFoe(player);
    }
    playerService.addFriend(player);
  }

  public void onRemoveFriendSelected() {
    playerService.removeFriend(getPlayer());
  }

  public void onReportPlayerSelected() {
    ReportDialogController reportDialogController = uiService.loadFxml("theme/reporting/report_dialog.fxml");
    chatUser.getPlayer().ifPresentOrElse(reportDialogController::setOffender,
        () -> reportDialogController.setOffender(getUsername()));
    reportDialogController.setOwnerWindow(contextMenuRoot.getOwnerWindow());
    reportDialogController.show();
  }

  public void onAddFoeSelected() {
    Player player = getPlayer();
    if (player.getSocialStatus() == FRIEND) {
      playerService.removeFriend(player);
    }
    playerService.addFoe(player);
  }

  public void onRemoveFoeSelected() {
    playerService.removeFoe(getPlayer());
  }

  public void onWatchGameSelected() {
    try {
      replayService.runLiveReplay(getPlayer().getGame().getId());
    } catch (Exception e) {
      log.error("Cannot display live replay", e.getCause());
      notificationService.addImmediateErrorNotification(e, "replays.live.loadFailure.message");
    }
  }

  public void onViewReplaysSelected() {
    eventBus.post(new ShowUserReplaysEvent( getPlayer().getId()));
  }

  public void onInviteToGameSelected() {
    teamMatchmakingService.invitePlayer(getPlayer().getUsername());
  }

  public void onBroadcastMessageSelected(ActionEvent actionEvent) {
    actionEvent.consume();

    TextInputDialog broadcastMessageInputDialog = new TextInputDialog();
    broadcastMessageInputDialog.setTitle(i18n.get("chat.userContext.broadcast"));

    broadcastMessageInputDialog.showAndWait()
        .ifPresent(broadcastMessage -> {
              if (broadcastMessage.isBlank()) {
                log.error("Broadcast message is empty: {}", broadcastMessage);
              } else {
                log.info("Sending broadcast message: {}", broadcastMessage);
                moderatorService.broadcastMessage(broadcastMessage);
              }
            }
        );
  }

  public void onJoinGameSelected() {
    joinGameHelper.join(getPlayer().getGame());
  }

  @NotNull
  private Player getPlayer() {
    return player != null ? player : chatUser.getPlayer()
        .orElseThrow(() -> new IllegalStateException("No player for chat user:" + chatUser));
  }

  private String getUsername() {
    return chatUser != null ? chatUser.getUsername() : player.getUsername();
  }

  public void onRemoveCustomColorSelected() {
    colorPicker.setValue(null);
  }

  @Override
  public ContextMenu getRoot() {
    return contextMenuRoot;
  }

  public void onKickGameSelected() {
    moderatorService.closePlayersGame(getPlayer().getId());
  }

  public void onKickLobbySelected() {
    moderatorService.closePlayersLobby(getPlayer().getId());
  }
}
