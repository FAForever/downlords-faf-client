package com.faforever.client.chat;

import javafx.scene.control.Label;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Represents a header in the chat user list, like "Moderators". */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatUserHeaderController implements ChatUserTreeItem<Label> {
  public Label titleLabel;

  public void setTitle(String title) {
    titleLabel.setText(title);
  }

  @Override
  public Label getRoot() {
    return titleLabel;
  }
}
