package com.faforever.client.chat;

import com.faforever.client.fx.Controller;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.faforever.client.chat.ChatColorMode.DEFAULT;

@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class ChatCategoryContextMenuController implements Controller<ContextMenu> {

  private final PreferencesService preferencesService;
  public CustomMenuItem colorPickerMenuItem;
  public ColorPicker colorPicker;
  public ContextMenu chatUserContextMenuRoot;
  public Button removeCustomColorButton;
  private Label chatCategoryLabel;

  public ChatCategoryContextMenuController(PreferencesService preferencesService) {
    this.preferencesService = preferencesService;
  }

  public void initialize() {
    removeCustomColorButton.managedProperty().bind(removeCustomColorButton.visibleProperty());
  }

  ContextMenu getContextMenu() {
    return chatUserContextMenuRoot;
  }


  public void setCategory(ChatUserCategory chatUserCategory) {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    colorPicker.setValue(chatPrefs.getGroupToColor().getOrDefault(chatUserCategory, null));

    colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        chatPrefs.getGroupToColor().remove(chatUserCategory);
      } else {
        chatPrefs.getGroupToColor().put(chatUserCategory, newValue);
      }
    });

    removeCustomColorButton.visibleProperty().bind(chatPrefs.chatColorModeProperty().isEqualTo(DEFAULT)
        .and(colorPicker.valueProperty().isNotNull()));
    colorPickerMenuItem.visibleProperty().bind(chatPrefs.chatColorModeProperty().isEqualTo(DEFAULT));
  }

  public void onRemoveCustomColor() {
    colorPicker.setValue(null);
  }

  @Override
  public ContextMenu getRoot() {
    return chatUserContextMenuRoot;
  }

  public void consumer(ActionEvent actionEvent) {
    actionEvent.consume();
  }
}
