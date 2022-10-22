package com.faforever.client.chat;

import com.faforever.client.theme.UiService;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.flowless.Cell;

@RequiredArgsConstructor
@Slf4j
public class ChatUserCategoryItem extends ListItem {

  private final ChatUserCategory category;
  private final ObservableList<ChatUserItem> userList;
  private final String channelName;

  @Override
  public Cell<ListItem, Node> createCell(UiService uiService) {
    return new Cell<>() {

      private Node node;
      private ChatCategoryItemController controller;

      @Override
      public Node getNode() {
        return node != null ? node : initializeNode();
      }

      private Node initializeNode() {
        controller = uiService.loadFxml("theme/chat/chat_user_category.fxml");
        controller.setChatUserCategory(category, channelName);
        controller.bindToUserList(userList);
        node = controller.getRoot();
        return node;
      }

      @Override
      public void dispose() {
        if (controller != null) {
          controller.dispose();
        }
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
