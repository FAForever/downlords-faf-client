package com.faforever.client.chat;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;

/** Represents a header in the chat user list, like "Moderators". */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ChatCategoryItemController implements Controller<Node> {
  private final I18n i18n;
  private final UiService uiService;
  private final PreferencesService preferencesService;
  public Label chatUserCategoryRoot;
  private WeakReference<ChatCategoryContextMenuController> contextMenuController = null;
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
    if (contextMenuController != null) {
      ChatCategoryContextMenuController controller = contextMenuController.get();
      if (controller != null) {
        controller.getContextMenu().show(chatUserCategoryRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        return;
      }
    }

    ChatCategoryContextMenuController controller = uiService.loadFxml("theme/chat/chat_category_context_menu.fxml");
    controller.setCategory(chatUserCategory);
    controller.getContextMenu().show(chatUserCategoryRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());

    contextMenuController = new WeakReference<>(controller);
  }

  @Override
  public Node getRoot() {
    return chatUserCategoryRoot;
  }
}
