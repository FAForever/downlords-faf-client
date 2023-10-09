package com.faforever.client.map;

import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.MapVersionReviewBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fa.FaStrings;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.HostGameEvent;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.PopupUtil;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.commons.io.Bytes;
import com.google.common.annotations.VisibleForTesting;
import io.micrometer.common.util.StringUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class MapDetailController extends NodeController<Node> {

  private final UiService uiService;
  private final MapService mapService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final TimeService timeService;
  private final PlayerService playerService;
  private final ReviewService reviewService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final ImageViewHelper imageViewHelper;
  private final NavigationHandler navigationHandler;
  private final ContextMenuBuilder contextMenuBuilder;

  private final ObjectProperty<MapVersionBean> mapVersion = new SimpleObjectProperty<>();
  private final ObservableList<MapVersionReviewBean> mapReviews = FXCollections.observableArrayList();

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
  public HBox hideBox;
  public Label mapIdLabel;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(uninstallButton, installButton, progressBar, progressLabel, hideButton,
                                    loadingContainer, hideBox, getRoot());
    JavaFxUtil.fixScrollSpeed(scrollPane);

    contextMenuBuilder.addCopyLabelContextMenu(nameLabel, authorLabel, mapDescriptionLabel, mapIdLabel);
    mapDetailRoot.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ESCAPE) {
        onCloseButtonClicked();
      }
    });
    mapVersion.addListener((SimpleChangeListener<MapVersionBean>) this::onMapVersionChanged);

    initializeReviewsController();
    bindProperties();
  }

  private void bindProperties() {
    ObservableValue<MapBean> mapObservable = mapVersion.flatMap(MapVersionBean::mapProperty);
    thumbnailImageView.imageProperty()
                      .bind(mapVersion.map(map -> mapService.loadPreview(map, PreviewSize.SMALL))
                                      .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
                                      .when(showing));
    nameLabel.textProperty().bind(mapObservable.flatMap(MapBean::displayNameProperty).when(showing));
    authorLabel.textProperty()
               .bind(mapObservable.flatMap(MapBean::authorProperty)
                                  .flatMap(PlayerBean::usernameProperty)
                                  .orElse(i18n.get("map.unknownAuthor"))
                                  .when(showing));
    maxPlayersLabel.textProperty()
                   .bind(mapVersion.flatMap(MapVersionBean::maxPlayersProperty).map(i18n::number).when(showing));
    mapIdLabel.textProperty()
              .bind(mapVersion.flatMap(MapVersionBean::idProperty).map(id -> i18n.get("map.id", id)).when(showing));
    mapPlaysLabel.textProperty()
                 .bind(mapObservable.flatMap(MapBean::gamesPlayedProperty).map(i18n::number).when(showing));
    versionPlaysLabel.textProperty()
                     .bind(mapVersion.flatMap(MapVersionBean::gamesPlayedProperty).map(i18n::number).when(showing));
    dimensionsLabel.textProperty()
                   .bind(mapVersion.flatMap(MapVersionBean::sizeProperty)
                                   .map(mapSize -> i18n.get("mapPreview.size", mapSize.getWidthInKm(),
                                                            mapSize.getHeightInKm()))
                                   .when(showing));
    dateLabel.textProperty()
             .bind(mapVersion.flatMap(MapVersionBean::createTimeProperty).map(timeService::asDate).when(showing));

    mapDescriptionLabel.textProperty()
                       .bind(mapVersion.flatMap(MapVersionBean::descriptionProperty)
                                       .map(FaStrings::removeLocalizationTag)
                                       .map(description -> StringUtils.isBlank(description) ? i18n.get(
                                           "map.noDescriptionAvailable") : description)
                                       .when(showing));

    mapVersionLabel.textProperty()
                   .bind(mapVersion.flatMap(MapVersionBean::versionProperty)
                                   .map(ComparableVersion::toString)
                                   .when(showing));

    BooleanExpression playerIsAuthor = BooleanExpression.booleanExpression(playerService.currentPlayerProperty()
                                                                                        .flatMap(
                                                                                            currentPlayer -> mapObservable.flatMap(
                                                                                                                              MapBean::authorProperty)
                                                                                                                          .map(
                                                                                                                              author -> Objects.equals(
                                                                                                                                  currentPlayer,
                                                                                                                                  author)))
                                                                                        .when(showing));
    BooleanExpression hidden = BooleanExpression.booleanExpression(mapVersion.flatMap(MapVersionBean::hiddenProperty));
    hideButton.visibleProperty().bind(Bindings.and(playerIsAuthor, hidden.not()).when(showing));
    isHiddenLabel.textProperty()
                 .bind(hidden.map(isHidden -> isHidden ? i18n.get("yes") : i18n.get("no")).when(showing));
    isRankedLabel.textProperty()
                 .bind(mapVersion.flatMap(MapVersionBean::rankedProperty)
                                 .map(isRanked -> isRanked ? i18n.get("yes") : i18n.get("no")));
    hideBox.visibleProperty().bind(playerIsAuthor.when(showing));

    ObservableValue<Double> hideRowSize = playerIsAuthor.map(isAuthor -> isAuthor ? Control.USE_COMPUTED_SIZE : 0d)
                                                        .when(showing);
    hideRow.prefHeightProperty().bind(hideRowSize);
    hideRow.maxHeightProperty().bind(hideRowSize);
    hideRow.minHeightProperty().bind(hideRowSize);

    BooleanExpression notOfficial = BooleanExpression.booleanExpression(mapVersion.map(mapService::isOfficialMap))
                                                     .not();
    BooleanExpression installed = mapService.isInstalledBinding(mapVersion);
    installButton.visibleProperty().bind(Bindings.and(notOfficial, installed.not()).when(showing));
    uninstallButton.visibleProperty().bind(Bindings.and(notOfficial, installed).when(showing));
    progressBar.visibleProperty()
               .bind(uninstallButton.visibleProperty().not().and(installButton.visibleProperty().not()));
    progressLabel.visibleProperty().bind(progressBar.visibleProperty());
    loadingContainer.visibleProperty().bind(progressBar.visibleProperty());
  }

  private void initializeReviewsController() {
    reviewsController.setCanWriteReview(false);
    reviewsController.setOnSendReviewListener(this::onSendReview);
    reviewsController.setOnDeleteReviewListener(this::onDeleteReview);
    reviewsController.setReviewSupplier(() -> {
      MapVersionReviewBean review = new MapVersionReviewBean();
      review.setPlayer(playerService.getCurrentPlayer());
      review.setMapVersion(mapVersion.get());
      return review;
    });
    reviewsController.bindReviews(mapReviews);
  }

  private void onMapVersionChanged(MapVersionBean newValue) {
    if (newValue == null) {
      reviewsController.setCanWriteReview(false);
      mapReviews.clear();
      installButton.setText("");
      return;
    }

    PlayerBean currentPlayer = playerService.getCurrentPlayer();
    if (!currentPlayer.equals(newValue.getMap().getAuthor())) {
      mapService.hasPlayedMap(currentPlayer, newValue)
                .publishOn(fxApplicationThreadExecutor.asScheduler())
                .subscribe(reviewsController::setCanWriteReview,
                           throwable -> log.error("Unable to set has played for review", throwable));
    } else {
      reviewsController.setCanWriteReview(false);
    }

    reviewService.getMapReviews(newValue.getMap())
                 .collectList()
                 .publishOn(fxApplicationThreadExecutor.asScheduler())
                 .subscribe(mapReviews::setAll, throwable -> log.error("Unable to populate reviews", throwable));

    mapService.getFileSize(newValue).thenAcceptAsync(mapFileSize -> {
      if (mapFileSize > -1) {
        installButton.setText(
            i18n.get("mapVault.installButtonFormat", Bytes.formatSize(mapFileSize, i18n.getUserSpecificLocale())));
      } else {
        installButton.setText(i18n.get("mapVault.install"));
      }
    }, fxApplicationThreadExecutor);
  }

  public void setMapVersion(MapVersionBean mapVersion) {
    this.mapVersion.set(mapVersion);
  }

  public void onCloseButtonClicked() {
    getRoot().setVisible(false);
  }

  @Override
  public Node getRoot() {
    return mapDetailRoot;
  }

  @VisibleForTesting
  void onDeleteReview(MapVersionReviewBean review) {
    reviewService.deleteMapVersionReview(review)
                 .publishOn(fxApplicationThreadExecutor.asScheduler())
                 .subscribe(null, throwable -> {
                   log.error("Review could not be deleted", throwable);
                   notificationService.addImmediateErrorNotification(throwable, "review.delete.error");
                 }, () -> mapReviews.remove(review));
  }

  @VisibleForTesting
  void onSendReview(MapVersionReviewBean review) {
    reviewService.saveMapVersionReview(review)
                 .filter(savedReview -> !mapReviews.contains(savedReview))
                 .publishOn(fxApplicationThreadExecutor.asScheduler())
                 .subscribe(savedReview -> {
                   mapReviews.remove(review);
                   mapReviews.add(savedReview);
                 }, throwable -> {
                   log.error("Review could not be saved", throwable);
                   notificationService.addImmediateErrorNotification(throwable, "review.save.error");
                 });
  }

  public void onInstallButtonClicked() {
    installMap();
  }

  public CompletableFuture<Void> installMap() {
    MapVersionBean mapVersion = this.mapVersion.get();
    return mapService.downloadAndInstallMap(mapVersion, progressBar.progressProperty(), progressLabel.textProperty())
                     .exceptionally(throwable -> {
                       log.error("Map installation failed", throwable);
                       notificationService.addImmediateErrorNotification(throwable, "mapVault.installationFailed",
                                                                         mapVersion.getMap().getDisplayName(),
                                                                         throwable.getLocalizedMessage());
                       return null;
                     });
  }

  public void onUninstallButtonClicked() {
    progressBar.progressProperty().unbind();
    progressBar.setProgress(-1);

    MapVersionBean mapVersion = this.mapVersion.get();
    mapService.uninstallMap(mapVersion).exceptionally(throwable -> {
      log.error("Could not delete map", throwable);
      notificationService.addImmediateErrorNotification(throwable, "mapVault.couldNotDeleteMap",
                                                        mapVersion.getMap().getDisplayName(),
                                                        throwable.getLocalizedMessage());
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
    MapVersionBean mapVersion = this.mapVersion.get();
    if (!mapService.isInstalled(mapVersion.getFolderName())) {
      installMap().thenRun(() -> navigationHandler.navigateTo(new HostGameEvent(mapVersion.getFolderName())));
    } else {
      navigationHandler.navigateTo(new HostGameEvent(mapVersion.getFolderName()));
    }
  }

  public void hideMap() {
    MapVersionBean mapVersion = this.mapVersion.get();
    mapService.hideMapVersion(mapVersion)
              .publishOn(fxApplicationThreadExecutor.asScheduler())
              .subscribe(null, throwable -> {
                log.error("Could not hide map", throwable);
                notificationService.addImmediateErrorNotification(throwable, "map.couldNotHide");
              }, () -> mapVersion.setHidden(true));
  }

  public void onMapPreviewImageClicked() {
    MapVersionBean map = mapVersion.get();
    if (map != null) {
      PopupUtil.showImagePopup(mapService.loadPreview(map, PreviewSize.LARGE));
    }
  }
}
