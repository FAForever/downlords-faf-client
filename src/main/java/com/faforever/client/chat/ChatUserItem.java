package com.faforever.client.chat;

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
    return new Cell<>() {

      private Node node;

      @Override
      public Node getNode() {
        if (node != null) {
          if (!user.isDisplayed()) {
            user.setDisplayed(true);
          }
          return node;
        } else {
          return initializeNode();
        }
      }

      @Override
      public void dispose() {
        user.setDisplayed(false);
      }

      private Node initializeNode() {
        ChatUserItemController chatUserItemController = uiService.loadFxml("theme/chat/chat_user_item.fxml");
        chatUserItemController.setChatUser(user);
        node = chatUserItemController.getRoot();
        return node;
      }
    };
  }

  @Override
  public Optional<ChatChannelUser> getUser() {
    return Optional.of(user);
  }

  @Override
  public ChatUserCategory getCategory() {
    return category;
  }

  @Override
  public boolean isCategory() {
    return false;
  }
}
