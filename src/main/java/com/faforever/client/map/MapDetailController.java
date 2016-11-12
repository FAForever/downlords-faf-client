package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.util.IdenticonUtil;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;

import javax.annotation.Resource;

import static java.util.Collections.singletonList;

public class MapDetailController {

  @FXML
  Label progressLabel;
  @FXML
  Button uninstallButton;
  @FXML
  Button installButton;
  @FXML
  ImageView thumbnailImageView;
  @FXML
  Label nameLabel;
  @FXML
  Label authorLabel;
  @FXML
  ProgressBar progressBar;
  @FXML
  Label mapDescriptionLabel;
  @FXML
  Node mapDetailRoot;

  @Resource
  MapService mapService;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  @Resource
  ReportingService reportingService;

  private MapBean map;
  private ListChangeListener<MapBean> installStatusChangeListener;

  @FXML
  void initialize() {
    uninstallButton.managedProperty().bind(uninstallButton.visibleProperty());
    installButton.managedProperty().bind(installButton.visibleProperty());
    progressBar.managedProperty().bind(progressBar.visibleProperty());
    progressBar.visibleProperty().bind(uninstallButton.visibleProperty().not().and(installButton.visibleProperty().not()));
    progressLabel.managedProperty().bind(progressLabel.visibleProperty());
    progressLabel.visibleProperty().bind(progressBar.visibleProperty());

    mapDetailRoot.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ESCAPE) {
        onCloseButtonClicked();
      }
    });

    installStatusChangeListener = change -> {
      while (change.next()) {
        for (MapBean mapBean : change.getAddedSubList()) {
          if (map.getFolderName().equalsIgnoreCase(mapBean.getFolderName())) {
            setInstalled(true);
            return;
          }
        }
        for (MapBean mapBean : change.getRemoved()) {
          if (map.getFolderName().equals(mapBean.getFolderName())) {
            setInstalled(false);
            return;
          }
        }
      }
    };
  }

  public void onCloseButtonClicked() {
    getRoot().setVisible(false);
  }

  private void setInstalled(boolean installed) {
    installButton.setVisible(!installed);
    uninstallButton.setVisible(installed);
  }

  public Node getRoot() {
    return mapDetailRoot;
  }

  public void setMap(MapBean map) {
    this.map = map;
    if (map.getLargeThumbnailUrl() != null) {
      thumbnailImageView.setImage(mapService.loadPreview(map, PreviewSize.LARGE));
    } else {
      thumbnailImageView.setImage(IdenticonUtil.createIdenticon(map.getId()));
    }
    nameLabel.setText(map.getDisplayName());
    authorLabel.setText(map.getAuthor());

    boolean mapInstalled = mapService.isInstalled(map.getFolderName());
    installButton.setVisible(!mapInstalled);
    uninstallButton.setVisible(mapInstalled);

    mapDescriptionLabel.textProperty().bind(map.descriptionProperty());
    mapService.getInstalledMaps().addListener(new WeakListChangeListener<>(installStatusChangeListener));
    setInstalled(mapService.isInstalled(map.getFolderName()));
  }

  @FXML
  void onInstallButtonClicked() {
    installButton.setVisible(false);

    mapService.downloadAndInstallMap(map, progressBar.progressProperty(), progressLabel.textProperty())
        .thenRun(() -> setInstalled(true))
        .exceptionally(throwable -> {
          notificationService.addNotification(new ImmediateNotification(
              i18n.get("errorTitle"),
              i18n.get("mapVault.installationFailed", map.getDisplayName(), throwable.getLocalizedMessage()),
              Severity.ERROR, throwable, singletonList(new ReportAction(i18n, reportingService, throwable))));
          setInstalled(false);
          return null;
        });
  }

  @FXML
  void onUninstallButtonClicked() {
    progressBar.progressProperty().unbind();
    progressBar.setProgress(-1);
    uninstallButton.setVisible(false);

    mapService.uninstallMap(map)
        .thenRun(() -> setInstalled(false))
        .exceptionally(throwable -> {
          notificationService.addNotification(new ImmediateNotification(
              i18n.get("errorTitle"),
              i18n.get("mapVault.couldNotDeleteMap", map.getDisplayName(), throwable.getLocalizedMessage()),
              Severity.ERROR, throwable, singletonList(new ReportAction(i18n, reportingService, throwable))));
          setInstalled(true);
          return null;
        });
  }

  @FXML
  void onDimmerClicked() {
    onCloseButtonClicked();
  }

  public void onContentPaneClicked(MouseEvent event) {
    event.consume();
  }
}
