package com.faforever.client.fx;


import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.faforever.client.moderator.ModeratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerInfoWindowController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.ClipboardUtil;
import com.google.common.eventbus.EventBus;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public abstract class AbstractPlayerContextMenuController implements Controller<ContextMenu> {

  protected final AvatarService avatarService;
  protected final EventBus eventBus;
  protected final I18n i18n;
  protected final JoinGameHelper joinGameHelper;
  protected final ModeratorService moderatorService;
  protected final NotificationService notificationService;
  protected final PlayerService playerService;
  protected final ReplayService replayService;
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

  private PlayerBean player;

  @Override
  public void initialize() {
    showUserInfo.setOnAction(event -> onShowUserInfoSelected());
    copyUsernameItem.setOnAction(event -> onCopyUsernameSelected());
    viewReplaysItem.setOnAction(event -> onViewReplaysSelected());
    copyUsernameItem.setVisible(true);
  }

  public ContextMenu getContextMenu() {
    return getRoot();
  }

  public void setPlayer(PlayerBean player) {
    this.player = player;
    showUserInfo.setVisible(true);
    viewReplaysItem.setVisible(true);
  }

  protected PlayerBean getPlayer() {
    return player;
  }

  public void onShowUserInfoSelected() {
    PlayerBean player = getPlayer();
    PlayerInfoWindowController playerInfoWindowController = uiService.loadFxml("theme/user_info_window.fxml");
    playerInfoWindowController.setPlayer(player);
    playerInfoWindowController.setOwnerWindow(getRoot().getOwnerWindow());
    playerInfoWindowController.show();
  }

  public void onCopyUsernameSelected() {
    PlayerBean player = getPlayer();
    ClipboardUtil.copyToClipboard(player.getUsername());
  }

  public void onViewReplaysSelected() {
    PlayerBean player = getPlayer();
    eventBus.post(new ShowUserReplaysEvent(player.getId()));
  }

  @Override
  public ContextMenu getRoot() {
    return playerContextMenuRoot;
  }

  public void consumer(ActionEvent actionEvent) {
    actionEvent.consume();
  }
}
