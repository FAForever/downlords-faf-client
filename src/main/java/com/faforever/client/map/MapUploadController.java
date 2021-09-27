package com.faforever.client.map;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.event.MapUploadedEvent;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.CopyErrorAction;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.GetHelpAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.CompletableTask;
import com.faforever.commons.api.dto.ApiException;
import com.faforever.commons.map.PreviewGenerator;
import com.google.common.eventbus.EventBus;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.faforever.client.notification.Severity.ERROR;
import static java.util.Arrays.asList;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class MapUploadController implements Controller<Node> {

  private final MapService mapService;
  private final ExecutorService executorService;
  private final NotificationService notificationService;
  private final ReportingService reportingService;
  private final PlatformService platformService;
  private final I18n i18n;
  private final EventBus eventBus;
  private final ClientProperties clientProperties;
  public Label rankedLabel;
  public Label uploadTaskMessageLabel;
  public Label uploadTaskTitleLabel;
  public Label sizeLabel;
  public Label playersLabel;
  public Pane parseProgressPane;
  public Pane uploadProgressPane;
  public Pane uploadCompletePane;
  public ProgressBar uploadProgressBar;
  public Pane mapInfoPane;
  public Label mapNameLabel;
  public Label descriptionLabel;
  public Label versionLabel;
  public ImageView thumbnailImageView;
  public Region mapUploadRoot;
  public CheckBox rankedCheckbox;
  public CheckBox rulesCheckBox;
  public Label rulesLabel;
  private Path mapPath;
  private MapVersionBean mapInfo;
  private CompletableTask<Void> uploadMapTask;
  private Runnable cancelButtonClickedListener;

  public MapUploadController(MapService mapService, ExecutorService executorService, NotificationService notificationService, ReportingService reportingService, PlatformService platformService, I18n i18n, EventBus eventBus, ClientProperties clientProperties) {
    this.mapService = mapService;
    this.executorService = executorService;
    this.notificationService = notificationService;
    this.reportingService = reportingService;
    this.platformService = platformService;
    this.i18n = i18n;
    this.eventBus = eventBus;
    this.clientProperties = clientProperties;
  }

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(mapInfoPane, uploadCompletePane, parseProgressPane, uploadProgressPane);

    mapInfoPane.setVisible(false);
    uploadProgressPane.setVisible(false);
    parseProgressPane.setVisible(false);
    uploadCompletePane.setVisible(false);

    rankedLabel.setLabelFor(rankedCheckbox);
  }

  public void setMapPath(Path mapPath) {
    this.mapPath = mapPath;
    enterParsingState();
    CompletableFuture.supplyAsync(() -> mapService.readMap(mapPath), executorService)
        .thenAccept(this::setMapInfo)
        .exceptionally(throwable -> {
          log.warn("Map could not be read", throwable);
          return null;
        });
  }

  private void enterParsingState() {
    mapInfoPane.setVisible(false);
    uploadProgressPane.setVisible(false);
    parseProgressPane.setVisible(true);
    uploadCompletePane.setVisible(false);
  }

  @SneakyThrows
  private static WritableImage generatePreview(Path mapPath) {
    return SwingFXUtils.toFXImage(PreviewGenerator.generatePreview(mapPath, 256, 256), new WritableImage(256, 256));
  }

  private void setMapInfo(MapVersionBean mapInfo) {
    this.mapInfo = mapInfo;
    enterMapInfoState();

    mapNameLabel.setText(mapInfo.getMap().getDisplayName());
    descriptionLabel.setText(mapInfo.getDescription());
    versionLabel.setText(Optional.ofNullable(mapInfo.getVersion()).map(ComparableVersion::toString).orElse(""));
    MapSize mapSize = mapInfo.getSize();
    sizeLabel.setText(i18n.get("mapVault.upload.sizeFormat", mapSize.getWidthInKm(), mapSize.getHeightInKm()));
    playersLabel.setText(i18n.get("mapVault.upload.playersFormat", mapInfo.getMaxPlayers()));

    thumbnailImageView.setImage(generatePreview(mapPath));
  }

  private void enterMapInfoState() {
    mapInfoPane.setVisible(true);
    uploadProgressPane.setVisible(false);
    parseProgressPane.setVisible(false);
    uploadCompletePane.setVisible(false);
  }

  public void onCancelUploadClicked() {
    uploadMapTask.cancel(true);
    enterMapInfoState();
  }

  private void onUploadFailed(Throwable throwable) {
    enterMapInfoState();
    if (throwable instanceof ApiException) {
      notificationService.addServerNotification(new ImmediateNotification(
          i18n.get("errorTitle"), i18n.get("mapVault.upload.failed", throwable.getLocalizedMessage()), ERROR,
          asList(
              new GetHelpAction(i18n, reportingService),
              new DismissAction(i18n)
          )
      ));
    } else {
      notificationService.addServerNotification(new ImmediateNotification(
          i18n.get("errorTitle"), i18n.get("mapVault.upload.failed", throwable.getLocalizedMessage()), ERROR, throwable,
          asList(
              new Action(i18n.get("mapVault.upload.retry"), event -> onUploadClicked()),
              new CopyErrorAction(i18n, reportingService, throwable),
              new GetHelpAction(i18n, reportingService),
              new DismissAction(i18n)
          )
      ));
    }
  }

  public void onUploadClicked() {
    if (!rulesCheckBox.isSelected()) {
      rulesLabel.getStyleClass().add("bad");
      return;
    }
    enterUploadingState();

    uploadProgressPane.setVisible(true);
    uploadMapTask = mapService.uploadMap(mapPath, rankedCheckbox.isSelected());
    uploadTaskTitleLabel.textProperty().bind(uploadMapTask.titleProperty());
    uploadTaskMessageLabel.textProperty().bind(uploadMapTask.messageProperty());
    uploadProgressBar.progressProperty().bind(uploadMapTask.progressProperty());

    uploadMapTask.getFuture()
        .thenAccept(v -> eventBus.post(new MapUploadedEvent(mapInfo)))
        .thenAccept(aVoid -> enterUploadCompleteState())
        .exceptionally(throwable -> {
          if (!(throwable instanceof CancellationException)) {
            onUploadFailed(throwable.getCause());
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

  public void onCancelClicked() {
    cancelButtonClickedListener.run();
  }

  public Region getRoot() {
    return mapUploadRoot;
  }

  public void setOnCancelButtonClickedListener(Runnable cancelButtonClickedListener) {
    this.cancelButtonClickedListener = cancelButtonClickedListener;
  }

  public void onShowRulesClicked() {
    platformService.showDocument(clientProperties.getVault().getMapRulesUrl());
  }

  public void onShowValidationClicked() {
    platformService.showDocument(clientProperties.getVault().getMapValidationUrl());
  }
}
