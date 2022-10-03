package com.faforever.client.chat;

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
    return new Cell<>() {

      private Node node;

      @Override
      public Node getNode() {
        return node != null ? node : initializeNode();
      }

      private Node initializeNode() {
        ChatCategoryItemController controller = uiService.loadFxml("theme/chat/chat_user_category.fxml");
        controller.setChatUserCategory(category, channelName);
        node = controller.getRoot();
        return node;
      }
    };
  }

  @Override
  public ChatUserCategory getCategory() {
    return category;
  }

  @Override
  public boolean isCategory() {
    return true;
  }
}
