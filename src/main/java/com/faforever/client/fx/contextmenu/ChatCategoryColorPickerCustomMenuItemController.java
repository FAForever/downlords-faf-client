package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.Assert;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.faforever.client.chat.ChatColorMode.RANDOM;

@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class ChatCategoryColorPickerCustomMenuItemController extends AbstractCustomMenuItemController<ChatUserCategory> {

  private final PreferencesService preferencesService;

  public ColorPicker colorPicker;
  public Button removeCustomColorButton;

  private ChatPrefs chatPrefs;

  public void initialize() {
    chatPrefs = preferencesService.getPreferences().getChat();
    removeCustomColorButton.setOnAction(event -> colorPicker.setValue(null));
    removeCustomColorButton.managedProperty().bind(removeCustomColorButton.visibleProperty());
    removeCustomColorButton.visibleProperty().bind(chatPrefs.chatColorModeProperty().isNotEqualTo(RANDOM)
        .and(colorPicker.valueProperty().isNotNull()));
    getRoot().visibleProperty().bind(chatPrefs.chatColorModeProperty().isNotEqualTo(RANDOM));
  }

  @Override
  public void afterSetObject() {
    Assert.checkNullIllegalState(object, "no chat category has been set");
    colorPicker.setValue(chatPrefs.getGroupToColor().getOrDefault(object, null));
    colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        chatPrefs.getGroupToColor().remove(object);
      } else {
        chatPrefs.getGroupToColor().put(object, newValue);
      }
    });
  }
}
