package com.faforever.client.chat;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.theme.UiService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.Cell;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatMessageCell implements Cell<ChatMessage, Node> {

  private final ChatMessageController chatMessageController;

  private final ObjectProperty<Consumer<ChatMessage>> onReplyButtonClicked = new SimpleObjectProperty<>();
  private final ObjectProperty<Consumer<ChatMessage>> onReplyClicked = new SimpleObjectProperty<>();
  private final ObjectProperty<ObservableList<ChatMessage>> items = new SimpleObjectProperty<>();
  private final IntegerProperty index = new SimpleIntegerProperty();

  public ChatMessageCell(UiService uiService, FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    chatMessageController = uiService.loadFxml("theme/chat/chat_message.fxml");
    ObservableValue<ChatMessage> previousMessageProperty = items.flatMap(
        items -> Bindings.valueAt(items, index.subtract(1)));
    chatMessageController.showDetailsProperty()
                         .bind(Bindings.createBooleanBinding(() -> showDetails(previousMessageProperty.getValue(),
                                                                               chatMessageController.getChatMessage()),
                                                             previousMessageProperty,
                                                             chatMessageController.chatMessageProperty()));
    chatMessageController.onReplyButtonClickedProperty().bind(onReplyButtonClicked);
    chatMessageController.onReplyClickedProperty().bind(onReplyClicked);
  }

  @Override
  public VBox getNode() {
    return chatMessageController.getRoot();
  }

  @Override
  public boolean isReusable() {
    return true;
  }

  @Override
  public void updateItem(ChatMessage item) {
    chatMessageController.setChatMessage(item);
  }

  @Override
  public void updateIndex(int index) {
    this.index.set(index);
  }

  @Override
  public void reset() {
    chatMessageController.setChatMessage(null);
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

  public ObservableList<ChatMessage> getItems() {
    return items.get();
  }

  public ObjectProperty<ObservableList<ChatMessage>> itemsProperty() {
    return items;
  }

  public void setItems(ObservableList<ChatMessage> items) {
    this.items.set(items);
  }
}
