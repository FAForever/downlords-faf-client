package com.faforever.client.map.management;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.theme.UiService;
import javafx.scene.control.ListCell;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class RemovableMapCell extends ListCell<MapVersionBean> {

  private final UiService uiService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private RemovableMapCellController removableMapCellController;

  @Override
  protected void updateItem(MapVersionBean mapVersion, boolean empty) {
    super.updateItem(mapVersion, empty);
    setText(null);

    if (mapVersion == null || empty) {
      fxApplicationThreadExecutor.execute(() -> setGraphic(null));
    } else {
      if (removableMapCellController == null) {
        removableMapCellController = uiService.loadFxml("theme/vault/map/removable_map_cell.fxml");
      }
      removableMapCellController.setMapVersion(mapVersion);
      fxApplicationThreadExecutor.execute(() -> setGraphic(removableMapCellController.getRoot()));
    }
  }
}
