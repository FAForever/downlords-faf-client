package com.faforever.client.map.management;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.NotificationService;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class RemovableMapCellController extends ListCell<MapVersionBean> implements Controller<Node> {

  public HBox root;
  public Button removeButton;
  public ImageView previewMapView;
  public Label mapNameLabel;

  private final MapService mapService;
  private final NotificationService notificationService;

  @Override
  protected void updateItem(MapVersionBean mapVersion, boolean empty) {
    super.updateItem(mapVersion, empty);
    JavaFxUtil.runLater(() -> {
      setText(null);

      if (mapVersion == null || empty) {
        setGraphic(null);
      } else {
        previewMapView.setImage(mapService.loadPreview(mapVersion.getFolderName(), PreviewSize.SMALL));
        mapNameLabel.setText(mapVersion.getMap().getDisplayName());
        if (mapService.isCustomMap(mapVersion)) {
          removeButton.setOnMouseClicked(event -> mapService.uninstallMap(mapVersion).exceptionally(throwable -> {
            log.error("cannot uninstall the map", throwable);
            notificationService.addImmediateErrorNotification(throwable, "management.maps.uninstall.error");
            return null;
          }));
        } else {
          removeButton.setDisable(true);
        }
        setGraphic(getRoot());
      }
    });
  }

  @Override
  public Node getRoot() {
    return root;
  }
}
