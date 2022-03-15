package com.faforever.client.chat.test;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import org.fxmisc.flowless.Cell;

import java.util.Optional;

public abstract class ListItem {

  public abstract Cell<ListItem, Node> createCell(UiService uiService);

  public abstract ChatUserCategory getCategory();

  public Optional<ChatChannelUser> getUser() {
    return Optional.empty();
  }
}
