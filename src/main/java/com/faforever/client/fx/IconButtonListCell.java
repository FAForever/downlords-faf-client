package com.faforever.client.fx;

import com.faforever.client.theme.UiService;
import javafx.scene.control.ListCell;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.function.Consumer;

/**
 * Alternative to {@link StringListCell} that allows to display to differently styled strings in each cell.
 */
@RequiredArgsConstructor
public class IconButtonListCell<T> extends ListCell<T> {
  private final Consumer<IconButtonListCellControllerAndItem<T>> configureControllerCallback;
  private final UiService uiService;
  private IconButtonListCellController iconButtonListCellController;

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);
    JavaFxUtil.runLater(() -> {
      if (empty || item == null) {
        setText(null);
        setGraphic(null);
      } else {
        if (iconButtonListCellController == null) {
          iconButtonListCellController = uiService.loadFxml("theme/iconButtonListCell.fxml");
        }
        configureControllerCallback.accept(new IconButtonListCellControllerAndItem<>(iconButtonListCellController, item));
        setGraphic(iconButtonListCellController.getRoot());
      }
    });
  }

  @Value
  public static class IconButtonListCellControllerAndItem<E> {
    IconButtonListCellController iconButtonListCellController;
    E item;
  }
}