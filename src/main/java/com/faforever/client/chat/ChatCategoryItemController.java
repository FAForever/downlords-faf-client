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
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
  public Label arrowLabel;
  public Label userCounterLabel;

  private ChatPrefs chatPrefs;

  private ChatUserCategory chatUserCategory;

  @Override
  public void afterPropertiesSet() throws Exception {
    chatPrefs = preferencesService.getPreferences().getChat();
  }

  void setChatUserCategory(ChatUserCategory chatUserCategory) {
    this.chatUserCategory = chatUserCategory;

    categoryLabel.setText(i18n.get(chatUserCategory.getI18nKey()));
    bindProperties();
    updateArrowLabel();
  }

  private void bindProperties() {
    categoryLabel.styleProperty().bind(chatPrefs.groupToColorProperty()
        .map(groupToColor -> groupToColor.get(chatUserCategory))
        .map(JavaFxUtil::toRgbCode)
        .map(color -> String.format("-fx-text-fill: %s", color))
        .orElse(""));
  }

  public void onCategoryClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 1) {
      updateArrowLabel();
    }
  }

  public void bindToUserList(ObservableList<ChatUserItem> userList) {
    userCounterLabel.textProperty().bind(Bindings.size(userList).asString());
  }

  private void updateArrowLabel() {
    JavaFxUtil.runLater(() -> arrowLabel.setText(arrowLabel.getText().equals("˅") ? "˃" : "˅"));
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
