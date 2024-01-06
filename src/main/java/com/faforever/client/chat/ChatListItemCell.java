package com.faforever.client.chat;

import com.faforever.client.game.GameTooltipController;
import com.faforever.client.theme.UiService;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
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

  public void installGameTooltip(GameTooltipController gameTooltipController, Tooltip tooltip) {
    chatUserItemController.installGameTooltip(gameTooltipController, tooltip);
  }

  @Override
  public boolean isReusable() {
    return true;
  }

  @Override
  public void reset() {
    chatCategoryItemController.channelNameProperty().unbind();
    chatCategoryItemController.numCategoryItemsProperty().unbind();
    chatCategoryItemController.setChatUserCategory(null);
    chatUserItemController.setChatUser(null);
  }

  @Override
  public void updateItem(ChatListItem chatListItem) {
    chatCategoryItemController.setChatUserCategory(chatListItem.category());
    chatUserItemController.setChatUser(chatListItem.user());

    ObservableValue<String> channelNameProperty = chatListItem.channelNameProperty();
    if (channelNameProperty != null) {
      chatCategoryItemController.channelNameProperty().bind(channelNameProperty);
    } else {
      chatCategoryItemController.channelNameProperty().unbind();
    }
    ObservableValue<Integer> numItems = chatListItem.numCategoryItemsProperty();
    if (numItems != null) {
      chatCategoryItemController.numCategoryItemsProperty().bind(numItems);
    } else {
      chatCategoryItemController.numCategoryItemsProperty().unbind();
    }
  }
  @Override
  public Node getNode() {
    boolean hasUser = chatUserItemController.getChatUser() != null;
    return hasUser ? chatUserItemController.getRoot() : chatCategoryItemController.getRoot();
  }
}
