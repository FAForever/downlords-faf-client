package com.faforever.client.chat;

import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import org.fxmisc.flowless.Cell;

import java.util.Optional;

public interface ChatListItem {

  Cell<ChatListItem, Node> createCell(UiService uiService);

  ChatUserCategory getCategory();

  boolean isCategory();

  Optional<ChatChannelUser> getUser();
}
