package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.event.MapUploadedEvent;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.reporting.ReportingService;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static com.faforever.client.io.Bytes.formatSize;
import static java.util.Arrays.asList;

public class MapUploadController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @FXML
  Label bytesLabel;
  @FXML
  Label sizeLabel;
  @FXML
  Label playersLabel;
  @FXML
  Pane parseProgressPane;
  @FXML
  Pane uploadProgressPane;
  @FXML
  Pane uploadCompletePane;
  @FXML
  ProgressBar uploadProgressBar;
  @FXML
  Pane mapInfoPane;
  @FXML
  Label mapNameLabel;
  @FXML
  Label descriptionLabel;
  @FXML
  Label versionLabel;
  @FXML
  ImageView thumbnailImageView;
  @FXML
  Region mapUploadRoot;
  @FXML
  CheckBox rankedCheckbox;
  @FXML
  Label rankedLabel;

  @Resource
  MapService mapService;
  @Resource
  ThreadPoolExecutor threadPoolExecutor;
  @Resource
  NotificationService notificationService;
  @Resource
  ReportingService reportingService;
  @Resource
  I18n i18n;
  @Resource
  EventBus eventBus;

  private Path mapPath;
  private CompletableFuture<Void> uploadMapFuture;
  private MapBean mapInfo;

  @FXML
  void initialize() {
    mapInfoPane.managedProperty().bind(mapInfoPane.visibleProperty());
    uploadProgressPane.managedProperty().bind(uploadProgressPane.visibleProperty());
    parseProgressPane.managedProperty().bind(parseProgressPane.visibleProperty());
    uploadCompletePane.managedProperty().bind(uploadCompletePane.visibleProperty());

    mapInfoPane.setVisible(false);
    uploadProgressPane.setVisible(false);
    parseProgressPane.setVisible(false);
    uploadCompletePane.setVisible(false);

    rankedLabel.setLabelFor(rankedCheckbox);
  }

  public void setMapPath(Path mapPath) {
    this.mapPath = mapPath;
    enterParsingState();
    CompletableFuture.supplyAsync(() -> mapService.readMap(mapPath), threadPoolExecutor)
        .thenAccept(this::setMapInfo)
        .exceptionally(throwable -> {
          logger.warn("Map could not be read", throwable);
          return null;
        });
  }

  private void enterParsingState() {
    mapInfoPane.setVisible(false);
    uploadProgressPane.setVisible(false);
    parseProgressPane.setVisible(true);
    uploadCompletePane.setVisible(false);
  }

  private void setMapInfo(MapBean mapInfo) {
    this.mapInfo = mapInfo;
    enterMapInfoState();

    mapNameLabel.textProperty().bind(mapInfo.displayNameProperty());
    descriptionLabel.textProperty().bind(mapInfo.descriptionProperty());
    versionLabel.textProperty().bind(mapInfo.versionProperty().asString());
    sizeLabel.textProperty().bind(Bindings.createStringBinding(
        () -> {
          MapSize mapSize = mapInfo.getSize();
          return i18n.get("mapVault.upload.sizeFormat", mapSize.getWidth(), mapSize.getHeight());
        }, mapInfo.sizeProperty())
    );
    playersLabel.textProperty().bind(Bindings.createStringBinding(
        () -> i18n.get("mapVault.upload.playersFormat", mapInfo.getPlayers()), mapInfo.playersProperty())
    );

    Image image = PreviewGenerator.generatePreview(mapPath, 256, 256);
    thumbnailImageView.setImage(image);
  }

  private void enterMapInfoState() {
    mapInfoPane.setVisible(true);
    uploadProgressPane.setVisible(false);
    parseProgressPane.setVisible(false);
    uploadCompletePane.setVisible(false);
  }

  @FXML
  void onCancelUploadClicked() {
    uploadMapFuture.cancel(true);
    enterMapInfoState();
  }

  private void onUploadFailed(Throwable throwable) {
    enterMapInfoState();
    notificationService.addNotification(new ImmediateNotification(
        i18n.get("errorTitle"),
        i18n.get("mapVault.upload.failed"),
        Severity.ERROR,
        throwable,
        asList(
            new Action(i18n.get("mapVault.upload.retry"), event -> onUploadClicked()),
            new ReportAction(i18n, reportingService, throwable),
            new DismissAction(i18n)
        )
    ));
  }

  @FXML
  void onUploadClicked() {
    enterUploadingState();
    Locale locale = i18n.getLocale();

    uploadProgressBar.setProgress(0);
    uploadProgressPane.setVisible(true);
    uploadMapFuture = mapService.uploadMap(mapPath,
        (written, total) -> {
          uploadProgressBar.setProgress((double) written / total);
          Platform.runLater(() -> bytesLabel.setText(i18n.get("bytesProgress", formatSize(written, locale), formatSize(total, locale))));
        },
        rankedCheckbox.isSelected())
        .thenAccept(v -> eventBus.post(new MapUploadedEvent(mapInfo)))
        .thenAccept(aVoid -> enterUploadCompleteState())
        .exceptionally(throwable -> {
          if (!(throwable instanceof CancellationException)) {
            onUploadFailed(throwable);
          }
          return null;
        });
  }

  private void enterUploadingState() {
    mapInfoPane.setVisible(false);
    uploadProgressPane.setVisible(true);
    parseProgressPane.setVisible(false);
    uploadCompletePane.setVisible(false);
  }

  private void enterUploadCompleteState() {
    mapInfoPane.setVisible(false);
    uploadProgressPane.setVisible(false);
    parseProgressPane.setVisible(false);
    uploadCompletePane.setVisible(true);
  }

  @FXML
  void onCancelClicked() {
    getRoot().getScene().getWindow().hide();
  }

  public Region getRoot() {
    return mapUploadRoot;
  }
}
