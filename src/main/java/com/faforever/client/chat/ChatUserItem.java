package com.faforever.client.chat;

import com.faforever.client.theme.UiService;
import javafx.scene.Node;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.fxmisc.flowless.Cell;

import java.util.Optional;

@RequiredArgsConstructor
@EqualsAndHashCode
public class ChatUserItem implements ChatListItem {

  private final ChatChannelUser user;
  private final ChatUserCategory category;

  @Override
  public Cell<ChatListItem, Node> createCell(UiService uiService) {
    return new ChatUserItemCell(uiService);
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

  private class ChatUserItemCell implements Cell<ChatListItem, Node> {

    @Getter
    private final Node node;
    private final ChatUserItemController controller;

    public ChatUserItemCell(UiService uiService) {
      controller = uiService.loadFxml("theme/chat/chat_user_item.fxml");
      controller.setChatUser(user);
      node = controller.getRoot();
    }
  }
}
