package com.faforever.client.chat;

import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import org.fxmisc.flowless.Cell;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public abstract class ChatListItem {

  public static Comparator<ChatListItem> getComparator() {
    return Comparator.comparing(ChatListItem::getCategory).thenComparing(item -> item.getUser()
        .map(user -> user.getUsername().toLowerCase(Locale.ROOT)).orElse(""));
  }

  public abstract Cell<ChatListItem, Node> createCell(UiService uiService);

  public abstract ChatUserCategory getCategory();

  public abstract boolean isCategory();

  public Optional<ChatChannelUser> getUser() {
    return Optional.empty();
  }
}
