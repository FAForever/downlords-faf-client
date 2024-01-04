package com.faforever.client.chat;

import com.faforever.client.theme.UiService;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatMessageItemCell extends ListCell<ChatMessage> {

  private final ChatMessageController chatMessageController;

  public ChatMessageItemCell(UiService uiService) {
    chatMessageController = uiService.loadFxml("theme/chat/chat_message.fxml");
    ObservableValue<ChatMessage> previousMessageProperty = listViewProperty().flatMap(ListView::itemsProperty)
                                                                             .flatMap(items -> Bindings.valueAt(items,
                                                                                                                indexProperty().subtract(
                                                                                                                    1)));
    chatMessageController.showDetailsProperty()
                         .bind(Bindings.createBooleanBinding(
                             () -> showDetails(previousMessageProperty.getValue(), getItem()), previousMessageProperty,
                             itemProperty()).when(emptyProperty().not()));
  }

  @Override
  protected void updateItem(ChatMessage item, boolean empty) {
    super.updateItem(item, empty);
    chatMessageController.setChatMessage(item);
    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      setGraphic(chatMessageController.getRoot());
      setText(null);
    }
  }

  private boolean showDetails(ChatMessage previousMessage, ChatMessage currentMessage) {
    if (currentMessage == null) {
      return false;
    }

    if (previousMessage == null) {
      return true;
    }

    return !Objects.equals(previousMessage.sender(), currentMessage.sender());
  }
}
