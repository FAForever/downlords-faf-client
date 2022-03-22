package com.faforever.client.chat;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.contextmenu.ChatCategoryColorPickerCustomMenuItemController;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Represents a header in the chat user list, like "Moderators". */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ChatCategoryItemController implements Controller<Node>, InitializingBean {

  private final I18n i18n;
  private final UiService uiService;
  private final PreferencesService preferencesService;
  private final ContextMenuBuilder contextMenuBuilder;

  public Node root;
  public Label categoryLabel;
  public Label latentLabel;

  private ChatPrefs chatPrefs;

  private ChatUserCategory chatUserCategory;
  private String channelName;
  private ObservableMap<String, ObservableList<ChatUserCategory>> channelNameToHiddenCategoriesProperty;

  @Override
  public void afterPropertiesSet() throws Exception {
    chatPrefs = preferencesService.getPreferences().getChat();
    channelNameToHiddenCategoriesProperty = chatPrefs.getChannelNameToHiddenCategories();
  }

  @Override
  public void initialize() {
    JavaFxUtil.bind(categoryLabel.styleProperty(), Bindings.createStringBinding(() -> {
      Color color = chatPrefs.getGroupToColor().getOrDefault(chatUserCategory, null);
      return color != null ? String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color)) : "";
    }, chatPrefs.groupToColorProperty()));
  }

  void setChatUserCategory(ChatUserCategory chatUserCategory, String channelName) {
    this.chatUserCategory = chatUserCategory;
    this.channelName = channelName;

    categoryLabel.setText(i18n.get(chatUserCategory.getI18nKey()));
    updateLatentLabel();
  }

  public void onCategoryClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 1) {
      ObservableList<ChatUserCategory> hiddenCategories = channelNameToHiddenCategoriesProperty.get(channelName);
      if (hiddenCategories == null) {
        channelNameToHiddenCategoriesProperty.put(channelName, FXCollections.observableArrayList(chatUserCategory));
      } else {
        if (hiddenCategories.contains(chatUserCategory)) {
          if (hiddenCategories.size() == 1) {
            channelNameToHiddenCategoriesProperty.remove(channelName);
          } else {
            hiddenCategories.remove(chatUserCategory);
          }
        } else {
          hiddenCategories.add(chatUserCategory);
        }
      }
      updateLatentLabel();
    }
  }

  private void updateLatentLabel() {
    boolean isHidden = Optional.ofNullable(channelNameToHiddenCategoriesProperty.get(channelName))
        .stream().anyMatch(hiddenCategories -> hiddenCategories.contains(chatUserCategory));
    JavaFxUtil.runLater(() -> latentLabel.setText(isHidden ? "˃" : "˅"));
  }

  public void onContextMenuRequested(ContextMenuEvent event) {
    contextMenuBuilder.newBuilder()
        .addCustomItem(uiService.loadFxml("theme/chat/color_picker_menu_item.fxml", ChatCategoryColorPickerCustomMenuItemController.class), chatUserCategory)
        .build()
        .show(categoryLabel.getScene().getWindow(), event.getScreenX(), event.getScreenY());
  }

  @Override
  public Node getRoot() {
    return root;
  }
}
