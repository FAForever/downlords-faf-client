package com.faforever.client.map.management;

import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.fx.NodeController;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.NotificationService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
public class RemovableMapCellController extends NodeController<HBox> {

  public HBox root;
  public Button removeButton;
  public ImageView previewMapView;
  public Label mapNameLabel;

  private final MapService mapService;
  private final NotificationService notificationService;

  private final ObjectProperty<MapVersionBean> mapVersion = new SimpleObjectProperty<>();

  @Override
  protected void onInitialize() {
    previewMapView.imageProperty()
                  .bind(mapVersion.flatMap(MapVersionBean::folderNameProperty)
                                  .map(folderName -> mapService.loadPreview(folderName, PreviewSize.SMALL)));
    mapNameLabel.textProperty()
                .bind(mapVersion.flatMap(MapVersionBean::mapProperty).flatMap(MapBean::displayNameProperty));
    removeButton.disableProperty().bind(mapVersion.map(mapService::isCustomMap).map(isCustom -> !isCustom));
    removeButton.onMouseClickedProperty()
                .bind(mapVersion.map(
                    mapVersion -> event -> mapService.uninstallMap(mapVersion).exceptionally(throwable -> {
                      log.error("Cannot uninstall map `{}`", mapVersion, throwable);
                      notificationService.addImmediateErrorNotification(throwable, "management.maps.uninstall.error");
                      return null;
                    })));
  }

  public void setMapVersion(MapVersionBean mapVersion) {
    this.mapVersion.set(mapVersion);
  }

  @Override
  public HBox getRoot() {
    return root;
  }
}
