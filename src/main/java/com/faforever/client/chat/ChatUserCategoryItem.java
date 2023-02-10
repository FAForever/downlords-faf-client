package com.faforever.client.chat;

import com.faforever.client.theme.UiService;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import lombok.RequiredArgsConstructor;
import org.fxmisc.flowless.Cell;

@RequiredArgsConstructor
public class ChatUserCategoryItem extends ChatListItem {

  private final ChatUserCategory category;
  private final ObservableList<ChatUserItem> userList;
  private final String channelName;

  @Override
  public Cell<ChatListItem, Node> createCell(UiService uiService) {
    return new ChatUserCatergoryItemCell(uiService);
  }

  @Override
  public ChatUserCategory getCategory() {
    return category;
  }

  @Override
  public boolean isCategory() {
    return true;
  }

  private class ChatUserCatergoryItemCell implements Cell<ChatListItem, Node> {

    private final Node node;

    public ChatUserCatergoryItemCell(UiService uiService) {
      ChatCategoryItemController controller = uiService.loadFxml("theme/chat/chat_user_category.fxml");
      controller.setChatUserCategory(category, channelName);
      controller.bindToUserList(userList);
      node = controller.getRoot();
    }

    @Override
    public Node getNode() {
      return node;
    }
  }
}
