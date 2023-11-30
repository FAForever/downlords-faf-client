package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.contextmenu.ChatCategoryColorPickerCustomMenuItemController;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.theme.UiService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Represents a header in the chat user list, like "Moderators". */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ChatCategoryItemController extends NodeController<Node> {

  private final I18n i18n;
  private final UiService uiService;
  private final ContextMenuBuilder contextMenuBuilder;
  private final ChatPrefs chatPrefs;

  private final ObjectProperty<ChatUserCategory> chatUserCategory = new SimpleObjectProperty<>();
  private final StringProperty channelName = new SimpleStringProperty();
  private final IntegerProperty numCategoryItems = new SimpleIntegerProperty();
  private final ObjectProperty<ObservableSet<ChatUserCategory>> channelHiddenCategories = new SimpleObjectProperty<>(FXCollections.observableSet());

  public Node root;
  public Label categoryLabel;
  public Label arrowLabel;
  public Label userCounterLabel;

  @Override
  protected void onInitialize() {
    bindProperties();
  }

  private void bindProperties() {
    categoryLabel.styleProperty()
        .bind(chatPrefs.groupToColorProperty()
            .flatMap(groupToColor -> chatUserCategory.map(groupToColor::get))
            .map(JavaFxUtil::toRgbCode)
            .map(color -> String.format("-fx-text-fill: %s", color))
            .orElse("")
            .when(showing));

    categoryLabel.textProperty().bind(chatUserCategory.map(ChatUserCategory::getI18nKey).map(i18n::get).when(showing));

    channelHiddenCategories.bind(Bindings.valueAt(chatPrefs.getChannelNameToHiddenCategories(), channelName)
        .orElse(FXCollections.observableSet())
        .when(showing));

    arrowLabel.textProperty()
        .bind(channelHiddenCategories.flatMap(hiddenCategories -> Bindings.createBooleanBinding(() -> hiddenCategories.contains(chatUserCategory.get()), hiddenCategories, chatUserCategory))
                                     .map(hidden -> hidden ? "˃" : "˅").when(showing));

    userCounterLabel.textProperty().bind(numCategoryItems.map(String::valueOf).when(showing));
  }

  public void onCategoryClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton() == MouseButton.PRIMARY) {
      ChatUserCategory category = chatUserCategory.get();
      if (category == null) {
        return;
      }

      ObservableSet<ChatUserCategory> hiddenCategories = channelHiddenCategories.get();

      if (hiddenCategories.contains(category)) {
        hiddenCategories.remove(category);
      } else {
        hiddenCategories.add(category);
      }

      String channel = channelName.get();
      if (channel != null) {
        if (hiddenCategories.isEmpty()) {
          chatPrefs.getChannelNameToHiddenCategories().remove(channel);
        } else {
          chatPrefs.getChannelNameToHiddenCategories().put(channel, hiddenCategories);
        }
      }
    }
  }

  public void onContextMenuRequested(ContextMenuEvent event) {
    ChatUserCategory category = chatUserCategory.get();
    if (category == null) {
      return;
    }

    contextMenuBuilder.newBuilder()
        .addCustomItem(uiService.loadFxml("theme/chat/color_picker_menu_item.fxml", ChatCategoryColorPickerCustomMenuItemController.class), category)
        .build()
        .show(categoryLabel.getScene().getWindow(), event.getScreenX(), event.getScreenY());
  }

  public ChatUserCategory getChatUserCategory() {
    return chatUserCategory.get();
  }

  public ObjectProperty<ChatUserCategory> chatUserCategoryProperty() {
    return chatUserCategory;
  }

  public void setChatUserCategory(ChatUserCategory chatUserCategory) {
    this.chatUserCategory.set(chatUserCategory);
  }

  public String getChannelName() {
    return channelName.get();
  }

  public StringProperty channelNameProperty() {
    return channelName;
  }

  public void setChannelName(String channelName) {
    this.channelName.set(channelName);
  }

  public int getNumCategoryItems() {
    return numCategoryItems.get();
  }

  public IntegerProperty numCategoryItemsProperty() {
    return numCategoryItems;
  }

  public void setNumCategoryItems(int numCategoryItems) {
    this.numCategoryItems.set(numCategoryItems);
  }

  @Override
  public Node getRoot() {
    return root;
  }
}
