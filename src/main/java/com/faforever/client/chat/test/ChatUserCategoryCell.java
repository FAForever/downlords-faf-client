package com.faforever.client.chat.test;

import com.faforever.client.chat.ChatCategoryItemController;
import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import org.fxmisc.flowless.Cell;

public class ChatUserCategoryCell implements Cell<ListItem, Node> {

  private final Node node;

  public ChatUserCategoryCell(ChatUserCategory category, String channelName, UiService uiService) {
    ChatCategoryItemController controller = uiService.loadFxml("theme/chat/chat_user_category.fxml");
    controller.setChatUserCategory(category, channelName);
    node = controller.getRoot();
  }

  @Override
  public Node getNode() {
    return node;
  }
}
