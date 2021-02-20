package com.faforever.client.map.management;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.map.MapBean;
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
public class RemovableMapCellController extends ListCell<MapBean> implements Controller<Node> {

  public HBox root;
  public Button removeButton;
  public ImageView previewMapView;
  public Label mapNameLabel;

  private final MapService mapService;
  private final NotificationService notificationService;

  @Override
  protected void updateItem(MapBean item, boolean empty) {
    super.updateItem(item, empty);
    JavaFxUtil.runLater(() -> {
      setText(null);

      if (item == null || empty) {
        setGraphic(null);
      } else {
        previewMapView.setImage(mapService.loadPreview(item.getFolderName(), PreviewSize.SMALL));
        mapNameLabel.setText(item.getDisplayName());
        if (mapService.isCustomMap(item)) {
          removeButton.setOnMouseClicked(event -> mapService.uninstallMap(item).exceptionally(throwable -> {
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
