package com.faforever.client.map;

import com.faforever.client.domain.MapReviewsSummaryBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.vault.review.StarsController;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class MapCardController implements Controller<Node> {

  private final MapService mapService;
  private final MapGeneratorService mapGeneratorService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ReportingService reportingService;

  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Node mapTileRoot;
  public Label authorLabel;
  public StarsController starsController;
  public Label numberOfReviewsLabel;
  public Label numberOfPlaysLabel;
  public Label sizeLabel;
  public Label maxPlayersLabel;
  public Button installButton;
  public Button uninstallButton;

  private MapVersionBean mapVersion;
  private Consumer<MapVersionBean> onOpenDetailListener;
  private ListChangeListener<MapVersionBean> installStatusChangeListener;
  private final InvalidationListener reviewsChangedListener = observable -> populateReviews();

  public void initialize() {
    installButton.managedProperty().bind(installButton.visibleProperty());
    uninstallButton.managedProperty().bind(uninstallButton.visibleProperty());
    installStatusChangeListener = change -> {
      while (change.next()) {
        for (MapVersionBean mapVersion : change.getAddedSubList()) {
          if (this.mapVersion.getFolderName().equalsIgnoreCase(mapVersion.getFolderName())) {
            setInstalled(true);
            return;
          }
        }
        for (MapVersionBean mapVersion : change.getRemoved()) {
          if (this.mapVersion.getFolderName().equals(mapVersion.getFolderName())) {
            setInstalled(false);
            return;
          }
        }
      }
    };
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
    nameLabel.setText(mapVersion.getMap().getDisplayName());
    authorLabel.setText(Optional.ofNullable(mapVersion.getMap().getAuthor()).map(PlayerBean::getUsername).orElse(i18n.get("map.unknownAuthor")));
    numberOfPlaysLabel.setText(i18n.number(mapVersion.getMap().getGamesPlayed()));

    MapSize size = mapVersion.getSize();
    sizeLabel.setText(i18n.get("mapPreview.size", size.getWidthInKm(), size.getHeightInKm()));
    maxPlayersLabel.setText(i18n.number(mapVersion.getMaxPlayers()));

    if (mapService.isOfficialMap(mapVersion.getFolderName())) {
      installButton.setVisible(false);
      uninstallButton.setVisible(false);
    } else {
      ObservableList<MapVersionBean> installedMaps = mapService.getInstalledMaps();
      JavaFxUtil.addListener(installedMaps, new WeakListChangeListener<>(installStatusChangeListener));
      setInstalled(mapService.isInstalled(mapVersion.getFolderName()));
    }

    ObservableList<MapVersionReviewBean> reviews = mapVersion.getReviews();
    JavaFxUtil.addListener(reviews, new WeakInvalidationListener(reviewsChangedListener));
    reviewsChangedListener.invalidated(reviews);
  }

  private void populateReviews() {
    MapReviewsSummaryBean mapReviewsSummary = mapVersion.getMap().getMapReviewsSummary();
    int numReviews;
    float avgScore;
    if (mapReviewsSummary == null) {
      numReviews = 0;
      avgScore = 0;
    } else {
      numReviews = mapReviewsSummary.getReviews();
      avgScore = mapReviewsSummary.getScore() / numReviews;
    }
    JavaFxUtil.runLater(() -> {
      numberOfReviewsLabel.setText(i18n.number(numReviews));
      starsController.setValue(avgScore);
    });
  }

  public void onInstallButtonClicked() {
    mapService.downloadAndInstallMap(mapVersion, null, null)
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

  private void setInstalled(boolean installed) {
    installButton.setVisible(!installed);
    uninstallButton.setVisible(installed);
  }

  public Node getRoot() {
    return mapTileRoot;
  }

  public void setOnOpenDetailListener(Consumer<MapVersionBean> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowMapDetail() {
    onOpenDetailListener.accept(mapVersion);
  }
}
