package com.faforever.client.chat;

import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import org.fxmisc.flowless.Cell;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatMessageItemCell implements Cell<ChatMessage, Node> {

  private final ChatMessageController chatMessageController;

  public ChatMessageItemCell(UiService uiService) {
    chatMessageController = uiService.loadFxml("theme/chat/chat_message.fxml");
  }

  @Override
  public boolean isReusable() {
    return true;
  }

  @Override
  public void reset() {
    chatMessageController.setChatMessage(null);
  }

  @Override
  public void updateItem(ChatMessage chatMessage) {
    chatMessageController.setChatMessage(chatMessage);
  }

  @Override
  public Node getNode() {
    return chatMessageController.getRoot();
  }
}
