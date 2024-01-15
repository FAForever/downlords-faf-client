package com.faforever.client.chat;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.theme.UiService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatMessageItemCell extends ListCell<ChatMessage> {

  private final ChatMessageController chatMessageController;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final ObjectProperty<Consumer<ChatMessage>> onReplyButtonClicked = new SimpleObjectProperty<>();
  private final ObjectProperty<Consumer<ChatMessage>> onReplyClicked = new SimpleObjectProperty<>();

  public ChatMessageItemCell(UiService uiService, FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    this.fxApplicationThreadExecutor = fxApplicationThreadExecutor;
    chatMessageController = uiService.loadFxml("theme/chat/chat_message.fxml");
    ObservableValue<ChatMessage> previousMessageProperty = listViewProperty().flatMap(ListView::itemsProperty)
                                                                             .flatMap(items -> Bindings.valueAt(items,
                                                                                                                indexProperty().subtract(
                                                                                                                    1)));
    chatMessageController.showDetailsProperty()
                         .bind(Bindings.createBooleanBinding(
                             () -> showDetails(previousMessageProperty.getValue(), getItem()), previousMessageProperty,
                             itemProperty()).when(emptyProperty().not()));
    chatMessageController.getRoot().maxWidthProperty().bind(widthProperty().subtract(20));
    chatMessageController.onReplyButtonClickedProperty().bind(onReplyButtonClicked);
    chatMessageController.onReplyClickedProperty().bind(onReplyClicked);
  }

  @Override
  protected void updateItem(ChatMessage item, boolean empty) {
    fxApplicationThreadExecutor.execute(() -> {
      super.updateItem(item, empty);
      chatMessageController.setChatMessage(item);
      if (empty || item == null) {
        setText(null);
        setGraphic(null);
      } else {
        setGraphic(chatMessageController.getRoot());
        setText(null);
      }
    });
  }

  private boolean showDetails(ChatMessage previousMessage, ChatMessage currentMessage) {
    if (currentMessage == null) {
      return false;
    }

    if (previousMessage == null) {
      return true;
    }

    return !Objects.equals(previousMessage.getSender(), currentMessage.getSender());
  }

  public Consumer<ChatMessage> getOnReplyButtonClicked() {
    return onReplyButtonClicked.get();
  }

  public ObjectProperty<Consumer<ChatMessage>> onReplyButtonClickedProperty() {
    return onReplyButtonClicked;
  }

  public void setOnReplyButtonClicked(Consumer<ChatMessage> onReplyButtonClicked) {
    this.onReplyButtonClicked.set(onReplyButtonClicked);
  }

  public Consumer<ChatMessage> getOnReplyClicked() {
    return onReplyClicked.get();
  }

  public ObjectProperty<Consumer<ChatMessage>> onReplyClickedProperty() {
    return onReplyClicked;
  }

  public void setOnReplyClicked(Consumer<ChatMessage> onReplyClicked) {
    this.onReplyClicked.set(onReplyClicked);
  }
}
