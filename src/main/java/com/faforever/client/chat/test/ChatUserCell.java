package com.faforever.client.chat.test;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatUserItemController;
import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import org.fxmisc.flowless.Cell;

public class ChatUserCell implements Cell<ListItem, Node> {

  private final ChatChannelUser user;
  private final Node node;

  public ChatUserCell(ChatChannelUser user, UiService uiService) {
    this.user = user;
    ChatUserItemController chatUserItemController = uiService.loadFxml("theme/chat/chat_user_item.fxml");
    chatUserItemController.setChatUser(user);
    node = chatUserItemController.getRoot();
  }

  @Override
  public void dispose() {
    user.setDisplayed(false);
  }

  @Override
  public Node getNode() {
    return node;
  }
}
