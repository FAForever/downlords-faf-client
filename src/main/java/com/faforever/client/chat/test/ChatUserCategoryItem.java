package com.faforever.client.chat.test;

import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import lombok.RequiredArgsConstructor;
import org.fxmisc.flowless.Cell;

@RequiredArgsConstructor
public class ChatUserCategoryItem extends ListItem {

  private final ChatUserCategory category;
  private final String channelName;

  @Override
  public Cell<ListItem, Node> createCell(UiService uiService) {
    return new ChatUserCategoryCell(category, channelName, uiService);
  }

  @Override
  public ChatUserCategory getCategory() {
    return category;
  }
}
