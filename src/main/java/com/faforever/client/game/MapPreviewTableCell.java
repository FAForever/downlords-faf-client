package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.theme.UiService;
import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MapPreviewTableCell extends TableCell<GameBean, Image> {

  private final ImageView imageVew;

  public MapPreviewTableCell(UiService uiService) {
    Controller<ImageView> controller = uiService.loadFxml("theme/vault/map/map_preview_table_cell.fxml");
    imageVew = controller.getRoot();
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

