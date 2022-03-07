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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
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
  private final ContextMenuBuilder contextMenuBuilder;

  public Node root;
  public Label categoryLabel;
  private ChatUserCategory chatUserCategory;

  void setChatUserCategory(ChatUserCategory chatUserCategory) {
    this.chatUserCategory = chatUserCategory;

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    categoryLabel.setText(i18n.get(chatUserCategory.getI18nKey()));
    JavaFxUtil.bind(categoryLabel.styleProperty(), Bindings.createStringBinding(() -> {
      Color color = chatPrefs.getGroupToColor().getOrDefault(chatUserCategory, null);
      return color != null ? String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color)) : "";
    }, chatPrefs.groupToColorProperty()));
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
