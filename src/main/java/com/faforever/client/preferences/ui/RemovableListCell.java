package com.faforever.client.preferences.ui;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.theme.UiService;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


/**
 * A {@link ListCell} containing a 'remove' button that removes the represented item from the parent {@link ListView}.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class RemovableListCell<T> extends ListCell<T> {

  private final UiService uiService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private RemovableListCellController<T> removableListCellController;

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    JavaFxUtil.assertApplicationThread();

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      if (removableListCellController == null) {
        removableListCellController = uiService.loadFxml("theme/settings/removable_cell.fxml");
        removableListCellController.setOnRemove(() -> getListView().getItems().remove(getItem()));
      }

      removableListCellController.setText(item.toString());
      setGraphic(removableListCellController.getRoot());
    }
  }

}
