package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.map.MapPreviewTableCellController;
import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import static com.faforever.client.theme.UiService.NO_IMAGE_AVAILABLE;

public class MapPreviewTableCell extends TableCell<GameBean, Image> {

  private final ImageView imageVew;
  private final UiService uiService;

  public MapPreviewTableCell(UiService uiService) {
    this.uiService = uiService;

    imageVew = uiService.<MapPreviewTableCellController>loadFxml("theme/vault/map/map_preview_table_cell.fxml").getRoot();
    ImageViewHelper.setPlaceholderImage(imageVew, uiService.getThemeImage(NO_IMAGE_AVAILABLE), true);
    setGraphic(imageVew);
  }

  @Override
  protected void updateItem(Image item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      imageVew.setImage(!item.isError() ? item : uiService.getThemeImage(NO_IMAGE_AVAILABLE));
      setGraphic(imageVew);
    }
  }
}

