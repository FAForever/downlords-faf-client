package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatUserCategory;
import com.faforever.client.chat.event.ChatUserColorChangeEvent;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.eventbus.EventBus;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.faforever.client.chat.ChatColorMode.RANDOM;
import static java.util.Locale.US;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ColorPickerMenuItemController extends AbstractCustomMenuItemController<ChatChannelUser> {

  public ColorPicker colorPicker;
  public Button removeCustomColorButton;

  private final PreferencesService preferencesService;
  private final EventBus eventBus;

  private ChatPrefs chatPrefs;

  @Override
  public void initialize() {
    chatPrefs = preferencesService.getPreferences().getChat();
    getRoot().visibleProperty().bind(chatPrefs.chatColorModeProperty().isNotEqualTo(RANDOM));
    removeCustomColorButton.visibleProperty().bind(chatPrefs.chatColorModeProperty().isNotEqualTo(RANDOM)
        .and(colorPicker.valueProperty().isNotNull()));
  }

  @Override
  public void afterSetObject() {
    ChatChannelUser chatUser =  getObject();
    colorPicker.setValue(chatPrefs.getUserToColor().getOrDefault(getLowerUsername(chatUser), null));

    colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
      ChatUserCategory userCategory;
      if (chatUser.isModerator()) {
        userCategory = ChatUserCategory.MODERATOR;
      } else {
        userCategory = chatUser.getSocialStatus().map(status -> switch (status) {
          case FRIEND -> ChatUserCategory.FRIEND;
          case FOE -> ChatUserCategory.FOE;
          default -> ChatUserCategory.OTHER;
        }).orElse(ChatUserCategory.OTHER);
      }
      if (newValue == null) {
        chatPrefs.getUserToColor().remove(getLowerUsername(chatUser));
        chatUser.setColor(chatPrefs.getGroupToColor().getOrDefault(userCategory, null));
      } else {
        chatPrefs.getUserToColor().put(getLowerUsername(chatUser), newValue);
        chatUser.setColor(newValue);
      }
      eventBus.post(new ChatUserColorChangeEvent(chatUser));
    });
  }

  private String getLowerUsername(ChatChannelUser chatUser) {
    return chatUser.getUsername().toLowerCase(US);
  }

  public void onRemoveCustomColor() {
    colorPicker.setValue(null);
  }
}

