package com.faforever.client.teammatchmaking;

import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.faforever.client.player.PlayerInfoWindowController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.ClipboardUtil;
import com.google.common.eventbus.EventBus;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.SELF;

@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class PartyMemberContextMenuController implements Controller<ContextMenu> {

  private final ClientProperties clientProperties;
  private final PlayerService playerService;
  private final EventBus eventBus;
  private final UiService uiService;
  private final PlatformService platformService;

  public MenuItem sendPrivateMessageItem;
  public SeparatorMenuItem socialSeparator;
  public MenuItem addFriendItem;
  public MenuItem removeFriendItem;
  public MenuItem addFoeItem;
  public MenuItem removeFoeItem;
  public MenuItem viewReplaysItem;
  public MenuItem reportItem;
  public ContextMenu partyMemberContextMenuRoot;
  public MenuItem showUserInfo;
  private PlayerBean player;


  public PartyMemberContextMenuController(ClientProperties clientProperties,
                                          PlayerService playerService,
                                          EventBus eventBus,
                                          UiService uiService,
                                          PlatformService platformService) {
    this.clientProperties = clientProperties;
    this.playerService = playerService;
    this.eventBus = eventBus;
    this.uiService = uiService;
    this.platformService = platformService;
  }

  public ContextMenu getContextMenu() {
    return partyMemberContextMenuRoot;
  }

  public void setPlayer(PlayerBean player) {
    this.player = player;

    sendPrivateMessageItem.visibleProperty().bind(player.socialStatusProperty().isNotEqualTo(SELF));
    addFriendItem.visibleProperty().bind(player.socialStatusProperty().isNotEqualTo(FRIEND).and(player.socialStatusProperty().isNotEqualTo(SELF))
    );
    removeFriendItem.visibleProperty().bind(player.socialStatusProperty().isEqualTo(FRIEND));
    addFoeItem.visibleProperty().bind(player.socialStatusProperty().isNotEqualTo(FOE).and(player.socialStatusProperty().isNotEqualTo(SELF)));
    removeFoeItem.visibleProperty().bind(player.socialStatusProperty().isEqualTo(FOE));
    reportItem.visibleProperty().bind(player.socialStatusProperty().isNotEqualTo(SELF));


    socialSeparator.visibleProperty().bind(addFriendItem.visibleProperty().or(
        removeFriendItem.visibleProperty().or(
            addFoeItem.visibleProperty().or(
                removeFoeItem.visibleProperty()))));
  }


  public void onShowUserInfoSelected() {
    PlayerInfoWindowController playerInfoWindowController = uiService.loadFxml("theme/user_info_window.fxml");
    playerInfoWindowController.setPlayer(player);
    playerInfoWindowController.setOwnerWindow(partyMemberContextMenuRoot.getOwnerWindow());
    playerInfoWindowController.show();
  }

  public void onSendPrivateMessageSelected() {
    eventBus.post(new InitiatePrivateChatEvent(player.getUsername()));
  }

  public void onCopyUsernameSelected() {
    ClipboardUtil.copyToClipboard(player.getUsername());
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

  public void onReport() {
    platformService.showDocument(clientProperties.getWebsite().getReportUrl());
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

  public void onViewReplaysSelected() {
    PlayerBean player = getPlayer();
    eventBus.post(new ShowUserReplaysEvent(player.getId()));
  }

  @NotNull
  private PlayerBean getPlayer() {
    return player;
  }

  @Override
  public ContextMenu getRoot() {
    return partyMemberContextMenuRoot;
  }

  public void consumer(ActionEvent actionEvent) {
    actionEvent.consume();
  }
}
