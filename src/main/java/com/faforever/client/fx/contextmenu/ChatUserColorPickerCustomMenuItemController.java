package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.util.Assert;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static com.faforever.client.chat.ChatColorMode.RANDOM;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class ChatUserColorPickerCustomMenuItemController extends AbstractColorPickerCustomMenuItemController<ChatChannelUser> {

  private final ChatPrefs chatPrefs;

  @Override
  protected void onInitialize() {
    removeCustomColorButton.setOnAction(event -> colorPicker.setValue(null));
    removeCustomColorButton.visibleProperty()
        .bind(chatPrefs.chatColorModeProperty()
            .map(chatColorMode -> chatColorMode != RANDOM)
            .flatMap(notRandom -> colorPicker.valueProperty().isNotNull().map(notNull -> notRandom && notNull)));
    JavaFxUtil.bindManagedToVisible(removeCustomColorButton);
  }

  @Override
  public void afterSetObject() {
    Assert.checkNullIllegalState(object, "no chat user has been set");
    colorPicker.setValue(chatPrefs.getUserToColor().getOrDefault(getLowerUsername(object), null));
    initializeListeners();
  }

  private void initializeListeners() {
    JavaFxUtil.addListener(colorPicker.valueProperty(), (observable) -> updateUserColor());
  }

  private void updateUserColor() {
    ChatUserCategory userCategory;
    if (object.isModerator()) {
      userCategory = ChatUserCategory.MODERATOR;
    } else {
      userCategory = object.getCategories()
          .stream()
          .filter(category -> category != ChatUserCategory.MODERATOR)
          .findFirst()
          .orElse(ChatUserCategory.OTHER);
    }
    Color newColor = colorPicker.getValue();
    if (newColor == null) {
      chatPrefs.getUserToColor().remove(getLowerUsername(object));
      object.setColor(chatPrefs.getGroupToColor().getOrDefault(userCategory, null));
    } else {
      chatPrefs.getUserToColor().put(getLowerUsername(object), newColor);
      object.setColor(newColor);
    }
  }

  @Override
  protected boolean isItemVisible() {
    return object != null && !chatPrefs.getChatColorMode().equals(RANDOM);
  }

  private String getLowerUsername(ChatChannelUser chatUser) {
    return chatUser.getUsername().toLowerCase(Locale.US);
  }
}

