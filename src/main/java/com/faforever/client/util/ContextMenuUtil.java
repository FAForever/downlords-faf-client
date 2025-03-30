package com.faforever.client.util;

import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.contextmenu.AddEditPlayerNoteMenuItem;
import com.faforever.client.fx.contextmenu.AddFoeMenuItem;
import com.faforever.client.fx.contextmenu.AddFriendMenuItem;
import com.faforever.client.fx.contextmenu.BroadcastMessageMenuItem;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.CopyUsernameMenuItem;
import com.faforever.client.fx.contextmenu.InvitePlayerMenuItem;
import com.faforever.client.fx.contextmenu.JoinGameMenuItem;
import com.faforever.client.fx.contextmenu.KickGameMenuItem;
import com.faforever.client.fx.contextmenu.KickLobbyMenuItem;
import com.faforever.client.fx.contextmenu.OpenClanUrlMenuItem;
import com.faforever.client.fx.contextmenu.RemoveFoeMenuItem;
import com.faforever.client.fx.contextmenu.RemoveFriendMenuItem;
import com.faforever.client.fx.contextmenu.RemovePlayerNoteMenuItem;
import com.faforever.client.fx.contextmenu.ReportPlayerMenuItem;
import com.faforever.client.fx.contextmenu.SendPrivateMessageClanLeaderMenuItem;
import com.faforever.client.fx.contextmenu.SendPrivateMessageMenuItem;
import com.faforever.client.fx.contextmenu.ShowPlayerInfoMenuItem;
import com.faforever.client.fx.contextmenu.ViewReplaysMenuItem;
import com.faforever.client.fx.contextmenu.WatchGameMenuItem;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;

public class ContextMenuUtil {
  public static ContextMenu createContextMenu(ContextMenuEvent event, Node targetNode,
                                              PlayerInfo playerInfo,
                                              UiService uiService,
                                              ContextMenuBuilder contextMenuBuilder) {
    if (playerInfo == null) return new ContextMenu();

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