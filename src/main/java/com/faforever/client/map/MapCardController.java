package com.faforever.client.map;

import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapReviewsSummaryBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.theme.UiService;
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
public class MapCardController extends VaultEntityCardController<MapVersionBean> {

  private final UiService uiService;
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

  private Consumer<MapVersionBean> onOpenDetailListener;

  @Override
  protected void onInitialize() {
    imageViewHelper.setDefaultPlaceholderImage(thumbnailImageView);
    JavaFxUtil.bindManagedToVisible(installButton, uninstallButton);

    thumbnailImageView.imageProperty()
        .bind(entity.map(mapVersionBean -> mapService.loadPreview(mapVersionBean, PreviewSize.SMALL))
            .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
            .when(showing));

    ObservableValue<MapBean> mapObservable = entity.flatMap(MapVersionBean::mapProperty);
    nameLabel.textProperty().bind(mapObservable.flatMap(MapBean::displayNameProperty).when(showing));
    versionLabel.textProperty()
        .bind(entity.flatMap(MapVersionBean::versionProperty)
            .map(ComparableVersion::getCanonical)
            .map(version -> i18n.get("versionFormat", version))
            .when(showing));
    authorLabel.textProperty()
        .bind(mapObservable.flatMap(MapBean::authorProperty)
            .flatMap(PlayerBean::usernameProperty)
            .orElse(i18n.get("map.unknownAuthor"))
            .when(showing));

    numberOfPlaysLabel.textProperty()
        .bind(mapObservable.flatMap(MapBean::gamesPlayedProperty).map(i18n::number).when(showing));

    sizeLabel.textProperty()
        .bind(entity.flatMap(MapVersionBean::sizeProperty)
            .map(size -> i18n.get("mapPreview.size", size.getWidthInKm(), size.getHeightInKm()))
            .when(showing));

    maxPlayersLabel.textProperty()
        .bind(entity.flatMap(MapVersionBean::maxPlayersProperty).map(i18n::number).when(showing));

    BooleanExpression isOfficialMap = BooleanExpression.booleanExpression(entity.map(mapService::isOfficialMap));
    BooleanExpression isMapInstalled = mapService.isInstalledBinding(entity);

    installButton.visibleProperty().bind(isOfficialMap.not().and(isMapInstalled.not()).when(showing));
    uninstallButton.visibleProperty().bind(isOfficialMap.not().and(isMapInstalled).when(showing));

    numberOfReviewsLabel.textProperty()
        .bind(mapObservable.flatMap(MapBean::mapReviewsSummaryProperty)
            .flatMap(MapReviewsSummaryBean::numReviewsProperty)
            .orElse(0)
            .map(i18n::number)
            .when(showing));
    starsController.valueProperty()
        .bind(mapObservable.flatMap(MapBean::mapReviewsSummaryProperty)
            .flatMap(reviewsSummary -> reviewsSummary.scoreProperty().divide(reviewsSummary.numReviewsProperty()))
            .when(showing));
  }

  public void onInstallButtonClicked() {
    MapVersionBean mapVersionBean = entity.get();
    mapService.downloadAndInstallMap(mapVersionBean, null, null).exceptionally(throwable -> {
      log.error("Map installation failed", throwable);
      notificationService.addImmediateErrorNotification(throwable, "mapVault.installationFailed", mapVersionBean.getMap()
          .getDisplayName(), throwable.getLocalizedMessage());
      return null;
    });
  }

  public void onUninstallButtonClicked() {
    MapVersionBean mapVersionBean = entity.get();
    mapService.uninstallMap(mapVersionBean).exceptionally(throwable -> {
      log.error("Could not delete map", throwable);
      notificationService.addImmediateErrorNotification(throwable, "mapVault.couldNotDeleteMap", mapVersionBean.getMap()
          .getDisplayName(), throwable.getLocalizedMessage());
      return null;
    });
  }

  @Override
  public Node getRoot() {
    return mapTileRoot;
  }

  public void setOnOpenDetailListener(Consumer<MapVersionBean> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowMapDetail() {
    onOpenDetailListener.accept(entity.get());
  }
}
