package com.faforever.client.fx;

import com.faforever.client.util.ClipboardUtil;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class LabelContextMenuController implements Controller<ContextMenu> {

  public MenuItem copyLabel;
  public ContextMenu labelContextMenuRoot;

  private Label label;

  public ContextMenu getContextMenu() {
    return labelContextMenuRoot;
  }

  public void setLabel(Label label) {
    this.label = label;
  }

  public void onCopy() {
    if (label != null) {
      ClipboardUtil.copyToClipboard(label.getText());
    }
  }

  @Override
  public ContextMenu getRoot() {
    return labelContextMenuRoot;
  }
}
