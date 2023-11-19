package com.faforever.client.preferences.ui;

import com.faforever.client.fx.NodeController;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


/**
 * A {@link javafx.scene.control.ListCell} containing a 'remove' button that removes the represented item from the
 * parent {@link ListView}.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RemovableListCellController<T> extends NodeController<Pane> {

  public Pane removableCellRoot;
  public Label label;

  private Runnable onRemove = null;

  public void onRemoveButtonClicked() {
    if (onRemove != null) {
      onRemove.run();
    }
  }

  public void setText(String text) {
    label.setText(text);
  }

  public void setOnRemove(Runnable onRemove) {
    this.onRemove = onRemove;
  }

  @Override
  public Pane getRoot() {
    return removableCellRoot;
  }
}
