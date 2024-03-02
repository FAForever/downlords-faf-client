package com.faforever.client.map;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.exception.AssetLoadException;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.CopyErrorAction;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.GetHelpAction;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ServerNotification;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.commons.api.dto.ApiException;
import com.faforever.commons.map.PreviewGenerator;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import static com.faforever.client.notification.Severity.ERROR;
import static java.util.Arrays.asList;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class MapUploadController extends NodeController<Node> {

  private final MapService mapService;
  private final ExecutorService executorService;
  private final NotificationService notificationService;
  private final ReportingService reportingService;
  private final PlatformService platformService;
  private final I18n i18n;
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
  private CompletableTask<Void> uploadMapTask;
  private Runnable cancelButtonClickedListener;
  private Runnable uploadListener;

  @Override
  protected void onInitialize() {
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
    CompletableFuture.supplyAsync(() -> {
          try {
            return mapService.readMap(mapPath);
          } catch (MapLoadException e) {
            throw new CompletionException(e);
          }
        }, executorService)
        .thenAccept(this::setMapInfo)
        .exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
          log.error("Map could not be read", throwable);
          notificationService.addImmediateErrorNotification(throwable, "mapVault.upload.readError");
          return null;
        });
  }

  private void enterParsingState() {
    mapInfoPane.setVisible(false);
    uploadProgressPane.setVisible(false);
    parseProgressPane.setVisible(true);
    uploadCompletePane.setVisible(false);
  }

  private WritableImage generatePreview(Path mapPath) {
    try {
      return SwingFXUtils.toFXImage(PreviewGenerator.generatePreview(mapPath, 256, 256), new WritableImage(256, 256));
    } catch (IOException e) {
      throw new AssetLoadException("Could not generate preview from", e, "map.preview.generateError", mapPath);
    }
  }

  private void setMapInfo(MapVersion mapInfo) {
    enterMapInfoState();

    mapNameLabel.setText(mapInfo.map().displayName());
    descriptionLabel.setText(mapInfo.description());
    versionLabel.setText(Optional.ofNullable(mapInfo.version()).map(ComparableVersion::toString).orElse(""));
    MapSize mapSize = mapInfo.size();
    sizeLabel.setText(i18n.get("mapVault.upload.sizeFormat", mapSize.widthInKm(), mapSize.heightInKm()));
    playersLabel.setText(i18n.get("mapVault.upload.playersFormat", mapInfo.maxPlayers()));

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
      notificationService.addNotification(new ServerNotification(
          i18n.get("errorTitle"), i18n.get("mapVault.upload.failed", throwable.getLocalizedMessage()), ERROR,
          asList(
              new GetHelpAction(i18n, reportingService),
              new DismissAction(i18n)
          )
      ));
    } else {
      notificationService.addNotification(new ServerNotification(
          i18n.get("errorTitle"), i18n.get("mapVault.upload.failed", throwable.getLocalizedMessage()), ERROR, throwable,
          asList(new Action(i18n.get("mapVault.upload.retry"), this::onUploadClicked),
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
                 .thenRun(() -> {
                   if (uploadListener != null) {
                     uploadListener.run();
                   }
                 })
        .thenRun(this::enterUploadCompleteState)
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

  @Override
  public Region getRoot() {
    return mapUploadRoot;
  }

  public void setOnCancelButtonClickedListener(Runnable cancelButtonClickedListener) {
    this.cancelButtonClickedListener = cancelButtonClickedListener;
  }

  public void setUploadListener(Runnable uploadListener) {
    this.uploadListener = uploadListener;
  }

  public void onShowRulesClicked() {
    platformService.showDocument(clientProperties.getVault().getMapRulesUrl());
  }

  public void onShowValidationClicked() {
    platformService.showDocument(clientProperties.getVault().getMapValidationUrl());
  }
}
