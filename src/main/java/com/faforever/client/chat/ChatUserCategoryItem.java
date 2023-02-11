package com.faforever.client.chat;

import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.fxmisc.flowless.Cell;

import java.util.Optional;

@RequiredArgsConstructor
@EqualsAndHashCode
public class ChatUserCategoryItem implements ChatListItem {

  private final ChatUserCategory category;

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

  @Override
  public Optional<ChatChannelUser> getUser() {
    return Optional.empty();
  }

  private class ChatUserCatergoryItemCell implements Cell<ChatListItem, Node> {

    private final Node node;

    public ChatUserCatergoryItemCell(UiService uiService) {
      ChatCategoryItemController controller = uiService.loadFxml("theme/chat/chat_user_category.fxml");
      controller.setChatUserCategory(category);
      node = controller.getRoot();
    }

    @Override
    public Node getNode() {
      return node;
    }
  }
}
