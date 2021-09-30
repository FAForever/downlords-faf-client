package com.faforever.client.map;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fa.FaStrings;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.HostGameEvent;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.commons.io.Bytes;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class MapDetailController implements Controller<Node> {

  private final MapService mapService;
  private final MapGeneratorService mapGeneratorService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final TimeService timeService;
  private final ReportingService reportingService;
  private final PlayerService playerService;
  private final ReviewService reviewService;
  private final UiService uiService;
  private final EventBus eventBus;

  public Label progressLabel;
  public Button uninstallButton;
  public Button installButton;
  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label authorLabel;
  public ProgressBar progressBar;
  public Label mapDescriptionLabel;
  public Label mapVersionLabel;
  public Node mapDetailRoot;
  public ScrollPane scrollPane;
  public Label dimensionsLabel;
  public Label maxPlayersLabel;
  public Label dateLabel;
  public Label isHiddenLabel;
  public Label isRankedLabel;
  public Label mapPlaysLabel;
  public Label versionPlaysLabel;
  public ReviewsController<MapVersionReviewBean> reviewsController;
  public VBox loadingContainer;
  public RowConstraints hideRow;
  public Button hideButton;
  public Button unrankButton;
  public HBox hideBox;
  public Label mapIdLabel;

  private MapVersionBean mapVersion;
  private ListChangeListener<MapVersionBean> installStatusChangeListener;

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(uninstallButton, installButton, progressBar, progressLabel, hideButton,
        unrankButton, loadingContainer, hideBox, getRoot());
    JavaFxUtil.addLabelContextMenus(uiService, nameLabel, authorLabel, mapDescriptionLabel, mapIdLabel);
    JavaFxUtil.fixScrollSpeed(scrollPane);
    progressBar.visibleProperty().bind(uninstallButton.visibleProperty().not().and(installButton.visibleProperty().not()));
    progressLabel.visibleProperty().bind(progressBar.visibleProperty());
    loadingContainer.visibleProperty().bind(progressBar.visibleProperty());

    reviewsController.setCanWriteReview(false);

    mapDetailRoot.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ESCAPE) {
        onCloseButtonClicked();
      }
    });

    installStatusChangeListener = change -> {
      while (change.next()) {
        for (MapVersionBean mapBean : change.getAddedSubList()) {
          if (mapVersion.getFolderName().equalsIgnoreCase(mapBean.getFolderName())) {
            setInstalled(true);
            return;
          }
        }
        for (MapVersionBean mapBean : change.getRemoved()) {
          if (mapVersion.getFolderName().equals(mapBean.getFolderName())) {
            setInstalled(false);
            return;
          }
        }
      }
    };

    reviewsController.setReviewSupplier(MapVersionReviewBean::new);
  }

  private void renewAuthorControls() {
    PlayerBean player = playerService.getCurrentPlayer();
    boolean viewerIsAuthor = player.equals(mapVersion.getMap().getAuthor());
    unrankButton.setVisible(viewerIsAuthor && mapVersion.getRanked());
    hideButton.setVisible(viewerIsAuthor && !mapVersion.getHidden());
    isHiddenLabel.setText(mapVersion.getHidden() ? i18n.get("yes") : i18n.get("no"));
    isRankedLabel.setText(mapVersion.getRanked() ? i18n.get("yes") : i18n.get("no"));
    removeHideRow(!viewerIsAuthor);
  }

  private void removeHideRow(boolean hide) {
    hideBox.setVisible(!hide);
    hideRow.setMaxHeight(hide ? 0d : Control.USE_COMPUTED_SIZE);
    hideRow.setPrefHeight(hide ? 0d : Control.USE_COMPUTED_SIZE);
    hideRow.setMinHeight(hide ? 0d : Control.USE_COMPUTED_SIZE);
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

  public void setMapVersion(MapVersionBean mapVersion) {
    this.mapVersion = mapVersion;
    Image image;
    if (mapVersion.getThumbnailUrlLarge() != null) {
      image = mapService.loadPreview(mapVersion.getThumbnailUrlLarge(), PreviewSize.LARGE);
    } else if (mapGeneratorService.isGeneratedMap(mapVersion.getMap().getDisplayName())) {
      image = mapService.loadPreview(mapVersion.getMap().getDisplayName(), PreviewSize.LARGE);
    } else {
      image = IdenticonUtil.createIdenticon(mapVersion.getId());
    }
    thumbnailImageView.setImage(image);
    renewAuthorControls();
    nameLabel.setText(mapVersion.getMap().getDisplayName());
    authorLabel.setText(Optional.ofNullable(mapVersion.getMap().getAuthor()).map(PlayerBean::getUsername).orElse(i18n.get("map.unknownAuthor")));
    maxPlayersLabel.setText(i18n.number(mapVersion.getMaxPlayers()));
    mapIdLabel.setText(i18n.get("map.id", mapVersion.getId()));
    mapPlaysLabel.setText(i18n.number(mapVersion.getMap().getGamesPlayed()));
    versionPlaysLabel.setText(i18n.number(mapVersion.getGamesPlayed()));

    MapSize mapSize = mapVersion.getSize();
    dimensionsLabel.setText(i18n.get("mapPreview.size", mapSize.getWidthInKm(), mapSize.getHeightInKm()));

    OffsetDateTime createTime = mapVersion.getCreateTime();
    dateLabel.setText(timeService.asDate(createTime));

    boolean mapInstalled = mapService.isInstalled(mapVersion.getFolderName());
    setInstalled(mapInstalled);

    PlayerBean player = playerService.getCurrentPlayer();

    reviewsController.setCanWriteReview(false);
    mapService.hasPlayedMap(player, mapVersion)
        .thenAccept(hasPlayed -> reviewsController.setCanWriteReview(hasPlayed
            && (mapVersion.getMap().getAuthor() == null || mapVersion.getMap().getAuthor() != player)));

    reviewsController.setOnSendReviewListener(this::onSendReview);
    reviewsController.setOnDeleteReviewListener(this::onDeleteReview);
    reviewsController.setReviews(mapVersion.getMap().getVersions().stream()
        .flatMap(version -> version.getReviews().stream())
        .collect(Collectors.toCollection(FXCollections::observableArrayList)));
    reviewsController.setOwnReview(mapVersion.getReviews().stream()
        .filter(review -> review.getPlayer().getId().equals(player.getId()))
        .findFirst().orElse(null));

    mapService.getFileSize(mapVersion)
        .thenAccept(mapFileSize -> JavaFxUtil.runLater(() -> {
          if (mapFileSize > -1) {
            installButton.setText(i18n.get("mapVault.installButtonFormat", Bytes.formatSize(mapFileSize, i18n.getUserSpecificLocale())));
            installButton.setDisable(false);
          } else {
            installButton.setText(i18n.get("notAvailable"));
            installButton.setDisable(true);
          }
        }));

    mapDescriptionLabel.setText(Optional.ofNullable(mapVersion.getDescription())
        .map(Strings::emptyToNull)
        .map(FaStrings::removeLocalizationTag)
        .orElseGet(() -> i18n.get("map.noDescriptionAvailable")));
    if (mapVersion.getVersion() != null) {
      mapVersionLabel.setText(mapVersion.getVersion().toString());
    }


    if (mapService.isOfficialMap(mapVersion.getFolderName())) {
      installButton.setVisible(false);
      uninstallButton.setVisible(false);
    } else {
      ObservableList<MapVersionBean> installedMaps = mapService.getInstalledMaps();
      JavaFxUtil.addListener(installedMaps, new WeakListChangeListener<>(installStatusChangeListener));
      setInstalled(mapService.isInstalled(mapVersion.getFolderName()));
    }
  }

  @VisibleForTesting
  void onDeleteReview(MapVersionReviewBean review) {
    reviewService.deleteMapVersionReview(review)
        .thenRun(() -> JavaFxUtil.runLater(() -> {
          mapVersion.getReviews().remove(review);
          reviewsController.setOwnReview(null);
        }))
        .exceptionally(throwable -> {
          log.warn("Review could not be deleted", throwable);
          notificationService.addImmediateErrorNotification(throwable, "review.delete.error");
          return null;
        });
  }

  @VisibleForTesting
  void onSendReview(MapVersionReviewBean review) {
    boolean isNew = review.getId() == null;
    PlayerBean player = playerService.getCurrentPlayer();
    review.setPlayer(player);
    review.setMapVersion(mapVersion);
    reviewService.saveMapVersionReview(review)
        .thenRun(() -> {
          if (isNew) {
            mapVersion.getReviews().add(review);
          }
          reviewsController.setOwnReview(review);
        })
        .exceptionally(throwable -> {
          log.warn("Review could not be saved", throwable);
          notificationService.addImmediateErrorNotification(throwable, "review.save.error");
          return null;
        });
  }

  public void onInstallButtonClicked() {
    installMap();
  }

  public CompletableFuture<Void> installMap() {
    return mapService.downloadAndInstallMap(mapVersion, progressBar.progressProperty(), progressLabel.textProperty())
        .thenRun(() -> setInstalled(true))
        .exceptionally(throwable -> {
          log.error("Map installation failed", throwable);
          notificationService.addImmediateErrorNotification(throwable, "mapVault.installationFailed",
              mapVersion.getMap().getDisplayName(), throwable.getLocalizedMessage());
          setInstalled(false);
          return null;
        });
  }

  public void onUninstallButtonClicked() {
    progressBar.progressProperty().unbind();
    progressBar.setProgress(-1);

    mapService.uninstallMap(mapVersion)
        .thenRun(() -> setInstalled(false))
        .exceptionally(throwable -> {
          log.error("Could not delete map", throwable);
          notificationService.addImmediateErrorNotification(throwable, "mapVault.couldNotDeleteMap",
              mapVersion.getMap().getDisplayName(), throwable.getLocalizedMessage());
          setInstalled(true);
          return null;
        });
  }

  public void onDimmerClicked() {
    onCloseButtonClicked();
  }

  public void onContentPaneClicked(MouseEvent event) {
    event.consume();
  }

  public void onCreateGameButtonClicked() {
    if (!mapService.isInstalled(mapVersion.getFolderName())) {
      installMap().thenRun(() -> eventBus.post(new HostGameEvent(mapVersion.getFolderName())));
    } else {
      eventBus.post(new HostGameEvent(mapVersion.getFolderName()));
    }
  }

  public void hideMap() {
    mapService.hideMapVersion(mapVersion).thenAccept(aVoid -> JavaFxUtil.runLater(() -> {
      mapVersion.setHidden(true);
      renewAuthorControls();
    })).exceptionally(throwable -> {
      log.error("Could not hide map", throwable);
      notificationService.addImmediateErrorNotification(throwable, "map.couldNotHide");
      return null;
    });
  }

  public void unrankMap() {
    mapService.unrankMapVersion(mapVersion)
        .thenAccept(aVoid -> JavaFxUtil.runLater(() -> {
          mapVersion.setRanked(false);
          renewAuthorControls();
        }))
        .exceptionally(throwable -> {
          log.error("Could not unrank map", throwable);
          notificationService.addImmediateErrorNotification(throwable, "map.couldNotUnrank");
          return null;
        });
  }
}
