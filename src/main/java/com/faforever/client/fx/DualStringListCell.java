package com.faforever.client.fx;

import com.faforever.client.theme.UiService;
import javafx.scene.control.ListCell;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

/**
 * Alternative to {@link StringListCell} that allows to display to differently styled strings in each cell.
 */
@RequiredArgsConstructor
public class DualStringListCell<T> extends ListCell<T> {
  private final Function<T, String> functionLeft;
  private final Function<T, String> functionRight;
  private final Function<T, String> functionWebViewToolTip;
  private final String styleClasses;
  private final UiService uiService;
  public DualStringListCellController dualStringListCellController;

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);
    JavaFxUtil.runLater(() -> {
      if (empty || item == null) {
        setText(null);
        setGraphic(null);
      } else {
        if (dualStringListCellController == null) {
          dualStringListCellController = uiService.loadFxml("theme/dual_string_list_cell.fxml");
        }
        dualStringListCellController.setLeftText(functionLeft.apply(item));
        dualStringListCellController.setWebViewToolTip(functionWebViewToolTip.apply(item));
        dualStringListCellController.setRightText(functionRight.apply(item));
        // copy font styles
        dualStringListCellController.applyFont(getFont());
        dualStringListCellController.applyStyleClass(styleClasses);
        setGraphic(dualStringListCellController.getRoot());
      }
    });
  }
}