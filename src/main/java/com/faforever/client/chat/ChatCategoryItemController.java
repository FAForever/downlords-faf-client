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
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.paint.Color;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Represents a header in the chat user list, like "Moderators". */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ChatCategoryItemController implements Controller<Node> {

  private final I18n i18n;
  private final UiService uiService;
  private final PreferencesService preferencesService;
  private final ApplicationContext applicationContext;

  public Label chatUserCategoryRoot;
  private ChatUserCategory chatUserCategory;

  void setChatUserCategory(@Nullable ChatUserCategory chatUserCategory) {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    JavaFxUtil.unbind(chatUserCategoryRoot.styleProperty());

    this.chatUserCategory = chatUserCategory;
    if (chatUserCategory == null) {
      chatUserCategoryRoot.setText(null);
      return;
    }

    chatUserCategoryRoot.setText(i18n.get(chatUserCategory.getI18nKey()));
    JavaFxUtil.bind(chatUserCategoryRoot.styleProperty(), Bindings.createStringBinding(() -> {
      Color color = chatPrefs.getGroupToColor().getOrDefault(chatUserCategory, null);
      if (color != null) {
        return String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color));
      } else {
        return "";
      }
    }, chatPrefs.groupToColorProperty()));
  }

  public void onContextMenuRequested(ContextMenuEvent event) {
    ContextMenuBuilder.newBuilder(applicationContext)
        .addCustomItem(uiService.loadFxml("theme/chat/color_picker_menu_item.fxml", ChatCategoryColorPickerCustomMenuItemController.class), chatUserCategory)
        .build()
        .show(chatUserCategoryRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());
  }

  @Override
  public Node getRoot() {
    return chatUserCategoryRoot;
  }
}
