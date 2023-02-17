package com.faforever.client.fx.contextmenu;

import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.util.Assert;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.faforever.client.chat.ChatColorMode.RANDOM;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ChangeUsernameColorMenuItem extends AbstractMenuItem<ChatChannelUser> {

  private final UiService uiService;
  private final I18n i18n;
  private final ContextMenuBuilder contextMenuBuilder;
  private final ChatPrefs chatPrefs;

  @Override
  protected void onClicked() {
    Assert.checkNullIllegalState(object, "no chat user has been set");
    Node node = getStyleableNode();
    Bounds screenBounds = node.localToScreen(node.getBoundsInLocal());
    contextMenuBuilder.newBuilder()
        .addCustomItem(uiService.loadFxml("theme/chat/color_picker_menu_item.fxml", ChatUserColorPickerCustomMenuItemController.class), object)
        .build()
        .show(StageHolder.getStage(), screenBounds.getMinX(), screenBounds.getMinY());
  }

  @Override
  protected boolean isDisplayed() {
    return object != null && !chatPrefs.getChatColorMode().equals(RANDOM);
  }

  @Override
  protected String getItemText() {
    return i18n.get("chat.userContext.changeColor");
  }
}
