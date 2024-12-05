package com.faforever.client.map.management;

import com.faforever.client.domain.api.Map;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.fx.NodeController;
import com.faforever.client.i18n.I18n;
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

import org.apache.maven.artifact.versioning.ComparableVersion;
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
  public Label mapVersionLabel;

  private final I18n i18n;
  private final MapService mapService;
  private final NotificationService notificationService;

  private final ObjectProperty<MapVersion> mapVersion = new SimpleObjectProperty<>();

  @Override
  protected void onInitialize() {
    previewMapView.imageProperty().bind(mapVersion.map(MapVersion::folderName)
                                  .map(folderName -> mapService.loadPreview(folderName, PreviewSize.SMALL)));
    mapNameLabel.textProperty().bind(mapVersion.map(MapVersion::map).map(Map::displayName));
    mapVersionLabel.textProperty()
                .bind(mapVersion.map(MapVersion::version)
                    .map(ComparableVersion::getCanonical)
                    .map(version -> i18n.get("versionFormat", version)));
    removeButton.disableProperty().bind(mapVersion.map(mapService::isCustomMap).map(isCustom -> !isCustom));
    removeButton.onMouseClickedProperty()
                .bind(mapVersion.map(
                    mapVersion -> event -> mapService.uninstallMap(mapVersion).subscribe(null, throwable -> {
                      log.error("Cannot uninstall map `{}`", mapVersion, throwable);
                      notificationService.addImmediateErrorNotification(throwable, "management.maps.uninstall.error");
                    })));
  }

  public void setMapVersion(MapVersion mapVersion) {
    this.mapVersion.set(mapVersion);
  }

  @Override
  public HBox getRoot() {
    return root;
  }
}
