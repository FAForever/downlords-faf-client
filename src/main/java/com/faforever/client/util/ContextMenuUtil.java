package com.faforever.client.util;

import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.contextmenu.*;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import org.springframework.stereotype.Component;

@Component
public class ContextMenuUtil {
  private final UiService uiService;
  private final ContextMenuBuilder contextMenuBuilder;

  public ContextMenuUtil(UiService uiService, ContextMenuBuilder contextMenuBuilder) {
    this.uiService = uiService;
    this.contextMenuBuilder = contextMenuBuilder;
  }

  public ContextMenu createContextMenu(ContextMenuEvent event, Node targetNode, PlayerInfo playerInfo) {
    if (playerInfo == null) {
      return new ContextMenu();
    }

    return contextMenuBuilder.newBuilder()
                             .addItem(ShowPlayerInfoMenuItem.class, playerInfo)
                             .addItem(SendPrivateMessageMenuItem.class, playerInfo.getUsername())
                             .addItem(CopyUsernameMenuItem.class, playerInfo.getUsername())
                             .addSeparator()
                             .addItem(SendPrivateMessageClanLeaderMenuItem.class, playerInfo)
                             .addItem(OpenClanUrlMenuItem.class, playerInfo)
                             .addSeparator()
                             .addItem(InvitePlayerMenuItem.class, playerInfo)
                             .addItem(AddFriendMenuItem.class, playerInfo)
                             .addItem(RemoveFriendMenuItem.class, playerInfo)
                             .addItem(AddFoeMenuItem.class, playerInfo)
                             .addItem(RemoveFoeMenuItem.class, playerInfo)
                             .addSeparator()
                             .addItem(AddEditPlayerNoteMenuItem.class, playerInfo)
                             .addItem(RemovePlayerNoteMenuItem.class, playerInfo)
                             .addSeparator()
                             .addItem(ReportPlayerMenuItem.class, playerInfo)
                             .addSeparator()
                             .addItem(JoinGameMenuItem.class, playerInfo)
                             .addItem(WatchGameMenuItem.class, playerInfo)
                             .addItem(ViewReplaysMenuItem.class, playerInfo)
                             .addSeparator()
                             .addItem(KickGameMenuItem.class, playerInfo)
                             .addItem(KickLobbyMenuItem.class, playerInfo)
                             .addItem(BroadcastMessageMenuItem.class)
                             .addCustomItem(uiService.loadFxml("theme/chat/avatar_picker_menu_item.fxml"), playerInfo)
                             .build();
  }
}