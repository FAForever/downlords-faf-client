package com.faforever.client.game;

import com.faforever.client.fx.FxmlLoader;
import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MapPreviewTableCell extends TableCell<Game, Image> {

  private final ImageView imageVew;

  public MapPreviewTableCell(FxmlLoader fxmlLoader) {
    imageVew = fxmlLoader.loadAndGetRoot("map_preview_table_cell.fxml", this);
    setGraphic(imageVew);
  }

  @Override
  protected void updateItem(Image item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      imageVew.setImage(item);
      setGraphic(imageVew);
    }
  }
}

