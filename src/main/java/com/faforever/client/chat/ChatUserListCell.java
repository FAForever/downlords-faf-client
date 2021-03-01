package com.faforever.client.chat;

import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import org.fxmisc.flowless.Cell;

public class ChatUserListCell implements Cell<CategoryOrChatUserListItem, Node> {

  private final Node node;
  private ChatUserItemController chatUserItemController;
  private ChatCategoryItemController chatUserCategoryController;

  public ChatUserListCell(CategoryOrChatUserListItem chatUserListItem, UiService uiService) {
    if (chatUserListItem.getUser() != null) {
      chatUserItemController = uiService.loadFxml("theme/chat/chat_user_item.fxml");
      chatUserItemController.setChatUser(chatUserListItem.getUser());
      node = chatUserItemController.getRoot();
    } else {
      chatUserCategoryController = uiService.loadFxml("theme/chat/chat_user_category.fxml");
      chatUserCategoryController.setChatUserCategory(chatUserListItem.getCategory());
      node = chatUserCategoryController.getRoot();
    }
  }

  @Override
  public void dispose() {
    if (chatUserItemController != null && chatUserItemController.getChatUser() != null) {
      chatUserItemController.getChatUser().setDisplayed(false);
    }
    if (chatUserCategoryController != null) {
      chatUserCategoryController.setChatUserCategory(null);
    }
  }

  @Override
  public Node getNode() {
    return node;
  }
}
