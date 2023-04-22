package com.faforever.client.chat;

import com.faforever.client.game.GameTooltipController;
import com.faforever.client.theme.UiService;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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

  private final ObjectProperty<ChatUserCategory> chatUserCategory = new SimpleObjectProperty<>();
  private final StringProperty channelName = new SimpleStringProperty();
  private final IntegerProperty numCategoryItems = new SimpleIntegerProperty();
  private final ObjectProperty<ChatChannelUser> chatUser = new SimpleObjectProperty<>();

  public ChatListItemCell(UiService uiService) {
    chatCategoryItemController = uiService.loadFxml("theme/chat/chat_user_category.fxml");
    chatUserItemController = uiService.loadFxml("theme/chat/chat_user_item.fxml");

    chatCategoryItemController.chatUserCategoryProperty().bind(chatUserCategory);
    chatCategoryItemController.channelNameProperty().bind(channelName);
    chatCategoryItemController.numCategoryItemsProperty().bind(numCategoryItems);
    chatUserItemController.chatUserProperty().bind(chatUser);
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
    chatUserCategory.set(null);
    channelName.unbind();
    numCategoryItems.unbind();
    chatUser.set(null);
  }

  @Override
  public void updateItem(ChatListItem chatListItem) {
    chatUserCategory.set(chatListItem.category());
    ObservableValue<String> channelNameProperty = chatListItem.channelNameProperty();
    if (channelNameProperty != null) {
      channelName.bind(channelNameProperty);
    } else {
      channelName.unbind();
    }
    ObservableValue<Integer> numItems = chatListItem.numCategoryItemsProperty();
    if (numItems != null) {
      numCategoryItems.bind(numItems);
    } else {
      numCategoryItems.unbind();
    }
    chatUser.set(chatListItem.user());
  }
  @Override
  public Node getNode() {
    boolean hasUser = chatUserItemController.getChatUser() != null;
    return hasUser ? chatUserItemController.getRoot() : chatCategoryItemController.getRoot();
  }
}
