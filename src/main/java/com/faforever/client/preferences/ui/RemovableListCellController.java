package com.faforever.client.preferences.ui;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeListCell;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Function;


/**
 * A {@link javafx.scene.control.ListCell} containing a 'remove' button that removes the represented item from the
 * parent {@link ListView}.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RemovableListCellController extends ListCell<String> implements Controller<Pane> {

  public Pane removableCellRoot;
  public Label label;

  @Override
  protected void updateItem(String item, boolean empty) {
    super.updateItem(item, empty);

    JavaFxUtil.assertApplicationThread();

    setText(null);
    if (empty || item == null) {
      setGraphic(null);
    } else {
      setGraphic(removableCellRoot);
      label.setText(item);
    }
  }

  public void onRemoveButtonClicked() {
    getListView().getItems().remove(getItem());
  }

  @Override
  public Pane getRoot() {
    return removableCellRoot;
  }
}
