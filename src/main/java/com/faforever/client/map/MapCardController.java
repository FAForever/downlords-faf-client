package com.faforever.client.map;

import com.faforever.client.domain.api.Map;
import com.faforever.client.domain.api.MapReviewsSummary;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.vault.VaultEntityCardController;
import com.faforever.client.vault.review.StarsController;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class MapCardController extends VaultEntityCardController<MapVersion> {

  private final MapService mapService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ImageViewHelper imageViewHelper;

  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label versionLabel;
  public Node mapTileRoot;
  public Label authorLabel;
  public StarsController starsController;
  public Label numberOfReviewsLabel;
  public Label numberOfPlaysLabel;
  public Label sizeLabel;
  public Label maxPlayersLabel;
  public Button installButton;
  public Button uninstallButton;

  private Consumer<MapVersion> onOpenDetailListener;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(installButton, uninstallButton);

    thumbnailImageView.imageProperty()
        .bind(entity.map(mapVersionBean -> mapService.loadPreview(mapVersionBean, PreviewSize.SMALL))
            .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
            .when(showing));

    ObservableValue<Map> mapObservable = entity.map(MapVersion::map);
    nameLabel.textProperty().bind(mapObservable.map(Map::displayName).when(showing));
    versionLabel.textProperty().bind(entity.map(MapVersion::version)
            .map(ComparableVersion::getCanonical)
            .map(version -> i18n.get("versionFormat", version))
            .when(showing));
    authorLabel.textProperty().bind(mapObservable.map(Map::author).flatMap(PlayerInfo::usernameProperty)
            .orElse(i18n.get("map.unknownAuthor"))
            .when(showing));

    numberOfPlaysLabel.textProperty().bind(mapObservable.map(Map::gamesPlayed).map(i18n::number).when(showing));

    sizeLabel.textProperty().bind(entity.map(MapVersion::size)
                                        .map(size -> i18n.get("mapPreview.size", size.widthInKm(), size.heightInKm()))
            .when(showing));

    maxPlayersLabel.textProperty().bind(entity.map(MapVersion::maxPlayers).map(i18n::number).when(showing));

    BooleanExpression isOfficialMap = BooleanExpression.booleanExpression(entity.map(mapService::isOfficialMap));
    BooleanExpression isMapInstalled = mapService.isInstalledBinding(entity);

    installButton.visibleProperty().bind(isOfficialMap.not().and(isMapInstalled.not()).when(showing));
    uninstallButton.visibleProperty().bind(isOfficialMap.not().and(isMapInstalled).when(showing));

    numberOfReviewsLabel.textProperty()
                        .bind(mapObservable.map(Map::mapReviewsSummary)
                                           .map(MapReviewsSummary::numReviews)
                                           .orElse(0)
                                           .map(i18n::number)
                                           .when(showing));
    starsController.valueProperty().bind(mapObservable.map(Map::mapReviewsSummary)
                           .map(reviewsSummary -> reviewsSummary.score() / reviewsSummary.numReviews())
            .when(showing));
  }

  public void onInstallButtonClicked() {
    MapVersion mapVersion = entity.get();
    mapService.downloadAndInstallMap(mapVersion, null, null).subscribe(null, throwable -> {
      log.error("Map installation failed", throwable);
      notificationService.addImmediateErrorNotification(throwable, "mapVault.installationFailed",
                                                        mapVersion.map().displayName(),
                                                        throwable.getLocalizedMessage());
    });
  }

  public void onUninstallButtonClicked() {
    MapVersion mapVersion = entity.get();
    mapService.uninstallMap(mapVersion).subscribe(null, throwable -> {
      log.error("Could not delete map", throwable);
      notificationService.addImmediateErrorNotification(throwable, "mapVault.couldNotDeleteMap",
                                                        mapVersion.map().displayName(),
                                                        throwable.getLocalizedMessage());
    });
  }

  @Override
  public Node getRoot() {
    return mapTileRoot;
  }

  public void setOnOpenDetailListener(Consumer<MapVersion> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowMapDetail() {
    onOpenDetailListener.accept(entity.get());
  }
}
