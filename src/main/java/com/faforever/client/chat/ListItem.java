package com.faforever.client.chat;

import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import org.fxmisc.flowless.Cell;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public abstract class ListItem {

  public static Comparator<ListItem> getComparator() {
    return Comparator.comparing(ListItem::getCategory).thenComparing(item -> item.getUser()
        .map(user -> user.getUsername().toLowerCase(Locale.ROOT)).orElse(""));
  }

  public abstract Cell<ListItem, Node> createCell(UiService uiService);

  public abstract ChatUserCategory getCategory();

  public abstract boolean isCategory();

  public Optional<ChatChannelUser> getUser() {
    return Optional.empty();
  }
}
