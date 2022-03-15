package com.faforever.client.chat.test;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import lombok.RequiredArgsConstructor;
import org.fxmisc.flowless.Cell;

import java.util.Optional;

@RequiredArgsConstructor
public class ChatUserItem extends ListItem {

  private final ChatChannelUser user;
  private final ChatUserCategory category;

  @Override
  public Cell<ListItem, Node> createCell(UiService uiService) {
    return new ChatUserCell(user, uiService);
  }

  @Override
  public Optional<ChatChannelUser> getUser() {
    return Optional.of(user);
  }

  @Override
  public ChatUserCategory getCategory() {
    return category;
  }
}
