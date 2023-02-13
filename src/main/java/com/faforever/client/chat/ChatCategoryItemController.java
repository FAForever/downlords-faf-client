package com.faforever.client.chat;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.contextmenu.ChatCategoryColorPickerCustomMenuItemController;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Represents a header in the chat user list, like "Moderators". */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ChatCategoryItemController implements Controller<Node>, InitializingBean {

  private final I18n i18n;
  private final UiService uiService;
  private final PreferencesService preferencesService;
  private final ContextMenuBuilder contextMenuBuilder;

  private final ObjectProperty<ChatUserCategory> chatUserCategory = new SimpleObjectProperty<>();
  private final StringProperty channelName = new SimpleStringProperty();
  private final IntegerProperty numCategoryItems = new SimpleIntegerProperty();
  private final SetProperty<ChatUserCategory> channelHiddenCategories = new SimpleSetProperty<>(FXCollections.observableSet());

  public Node root;
  public Label categoryLabel;
  public Label arrowLabel;
  public Label userCounterLabel;

  private ChatPrefs chatPrefs;

  @Override
  public void afterPropertiesSet() throws Exception {
    chatPrefs = preferencesService.getPreferences().getChat();
  }

  @Override
  public void initialize() {
    bindProperties();
  }

  public void setDetails(ChatUserCategory chatUserCategory, String channelName, ObservableValue<Integer> numCategoryItems) {
    this.chatUserCategory.set(chatUserCategory);
    this.channelName.set(channelName);
    if (numCategoryItems != null) {
      this.numCategoryItems.bind(numCategoryItems);
    } else {
      this.numCategoryItems.unbind();
    }
  }

  private void bindProperties() {
    categoryLabel.styleProperty()
        .bind(chatPrefs.groupToColorProperty()
            .flatMap(groupToColor -> chatUserCategory.map(groupToColor::get))
            .map(JavaFxUtil::toRgbCode)
            .map(color -> String.format("-fx-text-fill: %s", color))
            .orElse(""));
    categoryLabel.textProperty().bind(chatUserCategory.map(ChatUserCategory::getI18nKey).map(i18n::get));
    channelHiddenCategories.bind(chatPrefs.getChannelNameToHiddenCategories()
        .flatMap(nameToHiddenCategories -> channelName.map(channelName -> nameToHiddenCategories.getOrDefault(channelName, FXCollections.observableSet()))));
    arrowLabel.textProperty()
        .bind(channelHiddenCategories.flatMap(hiddenCategories -> chatUserCategory.map(hiddenCategories::contains)
            .map(hidden -> hidden ? "˃" : "˅")));
    userCounterLabel.textProperty().bind(numCategoryItems.map(String::valueOf));
  }

  public void onCategoryClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton() == MouseButton.PRIMARY) {
      ChatUserCategory category = chatUserCategory.get();
      if (category == null) {
        return;
      }

      if (channelHiddenCategories.contains(category)) {
        channelHiddenCategories.remove(category);
      } else {
        channelHiddenCategories.add(category);
      }

      String channel = channelName.get();
      if (channel != null) {
        if (channelHiddenCategories.isEmpty()) {
          chatPrefs.getChannelNameToHiddenCategories().remove(channel);
        } else {
          chatPrefs.getChannelNameToHiddenCategories().put(channel, channelHiddenCategories.get());
        }
        preferencesService.storeInBackground();
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

  @Override
  public Node getRoot() {
    return root;
  }
}
