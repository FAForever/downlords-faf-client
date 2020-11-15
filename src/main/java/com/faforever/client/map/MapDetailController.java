package com.faforever.client.map;

import com.faforever.client.fa.FaStrings;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.HostGameEvent;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.commons.io.Bytes;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class MapDetailController implements Controller<Node> {

  private final MapService mapService;
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
  public Node mapDetailRoot;
  public ScrollPane scrollPane;
  public Label dimensionsLabel;
  public Label maxPlayersLabel;
  public Label dateLabel;
  public Label isHiddenLabel;
  public Label isRankedLabel;
  public ReviewsController reviewsController;
  public VBox loadingContainer;
  public RowConstraints hideRow;
  public Button hideButton;
  public Button unrankButton;
  public HBox hideBox;
  public Label mapIdLabel;

  private MapBean map;
  private ListChangeListener<MapBean> installStatusChangeListener;

  public void initialize() {
    JavaFxUtil.addLabelContextMenus(uiService, nameLabel, authorLabel, mapDescriptionLabel, mapIdLabel);
    JavaFxUtil.fixScrollSpeed(scrollPane);
    uninstallButton.managedProperty().bind(uninstallButton.visibleProperty());
    installButton.managedProperty().bind(installButton.visibleProperty());
    progressBar.managedProperty().bind(progressBar.visibleProperty());
    progressBar.visibleProperty().bind(uninstallButton.visibleProperty().not().and(installButton.visibleProperty().not()));
    progressLabel.managedProperty().bind(progressLabel.visibleProperty());
    progressLabel.visibleProperty().bind(progressBar.visibleProperty());
    loadingContainer.visibleProperty().bind(progressBar.visibleProperty());
    hideButton.managedProperty().bind(hideButton.visibleProperty());
    unrankButton.managedProperty().bind(unrankButton.visibleProperty());
    hideBox.managedProperty().bind(hideBox.visibleProperty());

    reviewsController.setCanWriteReview(false);

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

  private void renewAuthorControls() {
    Optional<Player> currentPlayer = playerService.getCurrentPlayer();
    Player player = currentPlayer.orElseThrow(() -> new IllegalStateException("Player must be set in vault"));
    boolean viewerIsAuthor = map.getAuthor() != null && String.valueOf(player.getUsername()).equals(map.getAuthor());
    unrankButton.setVisible(viewerIsAuthor && map.isRanked());
    hideButton.setVisible(viewerIsAuthor && !map.isHidden());
    isHiddenLabel.setText(map.isHidden() ? i18n.get("yes") : i18n.get("no"));
    isRankedLabel.setText(map.isRanked() ? i18n.get("yes") : i18n.get("no"));
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

  public void setMap(MapBean map) {
    this.map = map;
    if (map.getLargeThumbnailUrl() != null) {
      thumbnailImageView.setImage(mapService.loadPreview(map, PreviewSize.LARGE));
    } else {
      thumbnailImageView.setImage(IdenticonUtil.createIdenticon(map.getId()));
    }
    renewAuthorControls();
    nameLabel.setText(map.getDisplayName());
    authorLabel.setText(Optional.ofNullable(map.getAuthor()).orElse(i18n.get("map.unknownAuthor")));
    maxPlayersLabel.setText(i18n.number(map.getPlayers()));
    mapIdLabel.setText(i18n.get("map.id", map.getId()));

    MapSize mapSize = map.getSize();
    dimensionsLabel.setText(i18n.get("mapPreview.size", mapSize.getWidthInKm(), mapSize.getHeightInKm()));

    LocalDateTime createTime = map.getCreateTime();
    dateLabel.setText(timeService.asDate(createTime));

    boolean mapInstalled = mapService.isInstalled(map.getFolderName());
    setInstalled(mapInstalled);

    Player player = playerService.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("No user is logged in"));

    reviewsController.setCanWriteReview(false);
    mapService.hasPlayedMap(player.getId(), map.getId())
        .thenAccept(hasPlayed -> reviewsController.setCanWriteReview(hasPlayed));

    reviewsController.setOnSendReviewListener(this::onSendReview);
    reviewsController.setOnDeleteReviewListener(this::onDeleteReview);
    reviewsController.setReviews(map.getReviews());
    reviewsController.setOwnReview(map.getReviews().stream()
        .filter(review -> review.getPlayer().getId() == player.getId())
        .findFirst());

    mapService.getFileSize(map.getDownloadUrl())
        .thenAccept(mapFileSize -> Platform.runLater(() -> {
          if (mapFileSize > -1) {
            installButton.setText(i18n.get("mapVault.installButtonFormat", Bytes.formatSize(mapFileSize, i18n.getUserSpecificLocale())));
            installButton.setDisable(false);
          } else {
            installButton.setText(i18n.get("notAvailable"));
            installButton.setDisable(true);
          }
        }));

    mapDescriptionLabel.setText(Optional.ofNullable(map.getDescription())
        .map(Strings::emptyToNull)
        .map(FaStrings::removeLocalizationTag)
        .orElseGet(() -> i18n.get("map.noDescriptionAvailable")));


    if (mapService.isOfficialMap(map.getFolderName())) {
      installButton.setVisible(false);
      uninstallButton.setVisible(false);
    } else {
      ObservableList<MapBean> installedMaps = mapService.getInstalledMaps();
      JavaFxUtil.addListener(installedMaps, new WeakListChangeListener<>(installStatusChangeListener));
      setInstalled(mapService.isInstalled(map.getFolderName()));
    }
  }

  private void onDeleteReview(Review review) {
    reviewService.deleteMapVersionReview(review)
        .thenRun(() -> Platform.runLater(() -> {
          map.getReviews().remove(review);
          reviewsController.setOwnReview(Optional.empty());
        }))
        // TODO display error to user
        .exceptionally(throwable -> {
          log.warn("Review could not be deleted", throwable);
          return null;
        });
  }

  private void onSendReview(Review review) {
    boolean isNew = review.getId() == null;
    Player player = playerService.getCurrentPlayer()
        .orElseThrow(() -> new IllegalStateException("No current player is available"));
    review.setPlayer(player);
    reviewService.saveMapVersionReview(review, map.getId())
        .thenRun(() -> {
          if (isNew) {
            map.getReviews().add(review);
          }
          reviewsController.setOwnReview(Optional.of(review));
        })
        // TODO display error to user
        .exceptionally(throwable -> {
          log.warn("Review could not be saved", throwable);
          return null;
        });
  }

  public void onInstallButtonClicked() {
    installMap();
  }

  public CompletableFuture<Void> installMap(){
    return mapService.downloadAndInstallMap(map, progressBar.progressProperty(), progressLabel.textProperty())
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
    progressBar.progressProperty().unbind();
    progressBar.setProgress(-1);

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

  public void onDimmerClicked() {
    onCloseButtonClicked();
  }

  public void onContentPaneClicked(MouseEvent event) {
    event.consume();
  }

  public void onCreateGameButtonClicked() {
    if (!mapService.isInstalled(map.getFolderName())) {
      installMap().thenRun(() -> eventBus.post(new HostGameEvent(map.getFolderName())));
    } else {
      eventBus.post(new HostGameEvent(map.getFolderName()));
    }
  }

  public void hideMap() {
    mapService.hideMapVersion(map).thenAccept(aVoid -> Platform.runLater(() -> {
      map.setHidden(true);
      renewAuthorControls();
    })).exceptionally(throwable -> {
      log.error("Could not hide map", throwable);
      notificationService.addImmediateErrorNotification(throwable, "map.couldNotHide");
      return null;
    });
  }

  public void unrankMap() {
    mapService.unrankMapVersion(map)
        .thenAccept(aVoid -> Platform.runLater(() -> {
          map.setRanked(false);
          renewAuthorControls();
        }))
        .exceptionally(throwable -> {
          log.error("Could not unrank map", throwable);
          notificationService.addImmediateErrorNotification(throwable, "map.couldNotUnrank");
          return null;
        });
  }
}
