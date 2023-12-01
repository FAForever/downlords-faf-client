package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.util.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.faforever.client.chat.ChatColorMode.DEFAULT;
import static com.faforever.client.chat.ChatColorMode.RANDOM;

@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@RequiredArgsConstructor
public class ChatCategoryColorPickerCustomMenuItemController extends AbstractColorPickerCustomMenuItemController<ChatUserCategory> {

  private final ChatPrefs chatPrefs;

  @Override
  protected void onInitialize() {
    removeCustomColorButton.setOnAction(event -> colorPicker.setValue(null));
    removeCustomColorButton.visibleProperty().bind(chatPrefs.chatColorModeProperty().flatMap(chatColorMode -> colorPicker.valueProperty().isNotNull().map(isNotNull -> isNotNull && RANDOM != chatColorMode)));
    JavaFxUtil.bindManagedToVisible(removeCustomColorButton);
  }

  @Override
  public void afterSetObject() {
    Assert.checkNullIllegalState(object, "no chat category has been set");
    colorPicker.setValue(chatPrefs.getGroupToColor().getOrDefault(object, null));
    JavaFxUtil.addListener(colorPicker.valueProperty(), (observable, oldValue, newValue) -> {
      if (newValue == null) {
        chatPrefs.getGroupToColor().remove(object);
      } else {
        chatPrefs.getGroupToColor().put(object, newValue);
      }
    });
  }

  @Override
  public boolean isItemVisible() {
    return chatPrefs.getChatColorMode() == DEFAULT;
  }
}
