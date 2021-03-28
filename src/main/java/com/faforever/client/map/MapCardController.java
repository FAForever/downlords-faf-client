package com.faforever.client.map;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.vault.review.Review;
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

  private MapBean map;
  private Consumer<MapBean> onOpenDetailListener;
  private ListChangeListener<MapBean> installStatusChangeListener;
  private final InvalidationListener reviewsChangedListener = observable -> populateReviews();

  public void initialize() {
    installButton.managedProperty().bind(installButton.visibleProperty());
    uninstallButton.managedProperty().bind(uninstallButton.visibleProperty());
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

  public void setMap(MapBean map) {
    this.map = map;
    Image image;
    if (map.getLargeThumbnailUrl() != null) {
      image = mapService.loadPreview(map.getLargeThumbnailUrl(), PreviewSize.LARGE);
    } else if (mapGeneratorService.isGeneratedMap(map.getDisplayName())) {
      image = mapService.loadPreview(map.getDisplayName(), PreviewSize.LARGE);
    } else {
      image = IdenticonUtil.createIdenticon(map.getId());
    }
    thumbnailImageView.setImage(image);
    nameLabel.setText(map.getDisplayName());
    authorLabel.setText(Optional.ofNullable(map.getAuthor()).orElse(i18n.get("map.unknownAuthor")));
    numberOfPlaysLabel.setText(i18n.number(map.getNumberOfPlays()));

    MapSize size = map.getSize();
    sizeLabel.setText(i18n.get("mapPreview.size", size.getWidthInKm(), size.getHeightInKm()));
    maxPlayersLabel.setText(i18n.number(map.getPlayers()));

    if (mapService.isOfficialMap(map.getFolderName())) {
      installButton.setVisible(false);
      uninstallButton.setVisible(false);
    } else {
      ObservableList<MapBean> installedMaps = mapService.getInstalledMaps();
      JavaFxUtil.addListener(installedMaps, new WeakListChangeListener<>(installStatusChangeListener));
      setInstalled(mapService.isInstalled(map.getFolderName()));
    }

    ObservableList<Review> reviews = map.getReviews();
    JavaFxUtil.addListener(reviews, new WeakInvalidationListener(reviewsChangedListener));
    reviewsChangedListener.invalidated(reviews);
  }

  private void populateReviews() {
    JavaFxUtil.runLater(() -> {
      numberOfReviewsLabel.setText(i18n.number(map.getReviewsSummary().getReviews()));
      starsController.setValue(map.getReviewsSummary().getScore() / map.getReviewsSummary().getReviews());
    });
  }

  public void onInstallButtonClicked() {
    mapService.downloadAndInstallMap(map, null, null)
        .thenRun(() -> setInstalled(true))
        .exceptionally(throwable -> {
          log.error("Map installation failed", throwable);
          notificationService.addImmediateErrorNotification(throwable, "mapVault.installationFailed",
              map.getDisplayName(), throwable.getLocalizedMessage());
          setInstalled(false);
          return null;
        });
  }

  public void onUninstallButtonClicked() {
    mapService.uninstallMap(map)
        .thenRun(() -> setInstalled(false))
        .exceptionally(throwable -> {
          log.error("Could not delete map", throwable);
          notificationService.addImmediateErrorNotification(throwable, "mapVault.couldNotDeleteMap",
              map.getDisplayName(), throwable.getLocalizedMessage());
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

  public void setOnOpenDetailListener(Consumer<MapBean> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowMapDetail() {
    onOpenDetailListener.accept(map);
  }
}
