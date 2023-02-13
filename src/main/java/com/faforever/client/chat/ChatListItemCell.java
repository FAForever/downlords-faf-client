package com.faforever.client.chat;

import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import org.fxmisc.flowless.Cell;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatListItemCell implements Cell<ChatListItem, Node> {

  private final ChatCategoryItemController chatCategoryItemController;
  private final ChatUserItemController chatUserItemController;

  public ChatListItemCell(UiService uiService) {
    chatCategoryItemController = uiService.loadFxml("theme/chat/chat_user_category.fxml");
    chatUserItemController = uiService.loadFxml("theme/chat/chat_user_item.fxml");
  }

  @Override
  public boolean isReusable() {
    return true;
  }

  @Override
  public void reset() {
    chatCategoryItemController.setDetails(null, null, null);
    chatUserItemController.setChatUser(null);
  }

  @Override
  public void updateItem(ChatListItem chatListItem) {
    chatCategoryItemController.setDetails(chatListItem.category(), chatListItem.channelName(), chatListItem.numCategoryItemsProperty());
    chatUserItemController.setChatUser(chatListItem.user());
  }
  @Override
  public Node getNode() {
    boolean hasUser = chatUserItemController.getChatUser() != null;
    return hasUser ? chatUserItemController.getRoot() : chatCategoryItemController.getRoot();
  }
}
