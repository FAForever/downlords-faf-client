package com.faforever.client.chat;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Represents a header in the chat user list, like "Moderators". */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatUserItemCategoryController implements Controller<Node> {
  private final I18n i18n;
  public Label chatUserCategoryRoot;

  public ChatUserItemCategoryController(I18n i18n) {
    this.i18n = i18n;
  }

  void setChatUserCategory(@Nullable ChatUserCategory chatUserCategory) {
    if (chatUserCategory == null) {
      chatUserCategoryRoot.setText(null);
      return;
    }

    chatUserCategoryRoot.setText(i18n.get(chatUserCategory.getI18nKey()));
  }

  @Override
  public Node getRoot() {
    return chatUserCategoryRoot;
  }
}
