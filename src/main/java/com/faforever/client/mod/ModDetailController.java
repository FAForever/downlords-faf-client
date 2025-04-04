package com.faforever.client.mod;

import com.faforever.client.domain.api.Mod;
import com.faforever.client.domain.api.ModVersion;
import com.faforever.client.domain.api.ModVersionReview;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fa.FaStrings;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.SimpleChangeListener;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.util.TimeService;
import com.faforever.client.util.ContextMenuUtil;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.commons.io.Bytes;
import com.google.common.annotations.VisibleForTesting;
import io.micrometer.common.util.StringUtils;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class ModDetailController extends NodeController<Node> {

  private final ModService modService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final TimeService timeService;
  private final ImageViewHelper imageViewHelper;
  private final ReviewService reviewService;
  private final PlayerService playerService;
  private final ContextMenuBuilder contextMenuBuilder;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final ContextMenuUtil contextMenuUtil;

  private final ObjectProperty<ModVersion> modVersion = new SimpleObjectProperty<>();
  private final ObservableList<ModVersionReview> modReviews = FXCollections.observableArrayList();

  public Label updatedLabel;
  public Label sizeLabel;
  public Label versionLabel;
  public Label dependenciesTitle;
  public VBox dependenciesContainer;
  public ScrollPane scrollPane;
  public Button uninstallButton;
  public Button installButton;
  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label idLabel;
  public Label uploaderLabel;
  public Label modDescriptionLabel;
  public Node modDetailRoot;
  public ReviewsController<ModVersionReview> reviewsController;
  public VBox authorContainer;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(uninstallButton, installButton, getRoot());
    JavaFxUtil.fixScrollSpeed(scrollPane);

    contextMenuBuilder.addCopyLabelContextMenu(nameLabel, idLabel, versionLabel);
    uploaderLabel.setOnContextMenuRequested(this::onContextMenuRequested);
    modDetailRoot.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ESCAPE) {
        onCloseButtonClicked();
      }
    });

    initializeReviewsController();
    bindProperties();
    modVersion.addListener((SimpleChangeListener<ModVersion>) this::onModVersionChanged);

    // TODO hidden until dependencies are available
    dependenciesTitle.setManaged(false);
    dependenciesContainer.setManaged(false);
  }

  private void bindProperties() {
    ObservableValue<Mod> modObservable = modVersion.map(ModVersion::mod);
    thumbnailImageView.imageProperty()
        .bind(modVersion.map(modService::loadThumbnail)
            .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
            .when(showing));
    nameLabel.textProperty().bind(modObservable.map(Mod::displayName).when(showing));
    modObservable.map(Mod::author)
                 .when(showing)
                 .subscribe(authorString -> {
                   authorContainer.getChildren().clear();
                   if (authorString == null || authorString.isBlank()) {
                     return;
                   }
                   String[] authors = authorString.split(",");
                   for (String author : authors) {
                     String trimmedAuthor = author.trim();
                     Label authorLabel = new Label(trimmedAuthor);
                     authorLabel.getStyleClass().add("clickable");
                     authorLabel.setOnContextMenuRequested(event -> {
                       event.consume();
                       playerService.getPlayerByName(trimmedAuthor).subscribe(playerInfo -> {
                         if (playerInfo != null) {
                           Platform.runLater(() -> {
                             ContextMenu contextMenu = contextMenuUtil.createContextMenu(event, getRoot(), playerInfo);
                             contextMenu.show(getRoot().getScene().getWindow(), event.getScreenX(), event.getScreenY());
                           });
                         }
                       });
                     });

                     authorContainer.getChildren().add(authorLabel);
                   }
                 });
    uploaderLabel.textProperty().bind(modObservable.map(Mod::uploader).flatMap(PlayerInfo::usernameProperty)
            .map(author -> author)
            .when(showing));
    idLabel.textProperty().bind(modVersion.map(ModVersion::id).map(Object::toString).when(showing));

    updatedLabel.textProperty().bind(modVersion.map(ModVersion::createTime).map(timeService::asDate).when(showing));

    modDescriptionLabel.textProperty().bind(modVersion.map(ModVersion::description)
        .map(FaStrings::removeLocalizationTag)
        .map(description -> StringUtils.isBlank(description) ? i18n.get("map.noDescriptionAvailable") : description)
        .when(showing));

    versionLabel.textProperty()
                .bind(modVersion.map(ModVersion::version).map(ComparableVersion::toString).when(showing));

    BooleanExpression installed = modService.isInstalledBinding(modVersion);
    installButton.visibleProperty().bind(installed.not().when(showing));
    uninstallButton.visibleProperty().bind(installed.when(showing));
  }

  public void onCloseButtonClicked() {
    getRoot().setVisible(false);
  }

  @Override
  public Node getRoot() {
    return modDetailRoot;
  }

  public void onContextMenuRequested(ContextMenuEvent event) {
    event.consume();
    modVersion.map(ModVersion::mod).map(Mod::uploader).subscribe(playerInfo -> {
      ContextMenu contextMenu = contextMenuUtil.createContextMenu(event, getRoot(), playerInfo);
      contextMenu.show(getRoot().getScene().getWindow(), event.getScreenX(), event.getScreenY());
    });
  }

  public void setModVersion(ModVersion modVersion) {
    this.modVersion.set(modVersion);
  }

  private void onModVersionChanged(ModVersion newValue) {
    if (newValue == null) {
      reviewsController.setCanWriteReview(false);
      modReviews.clear();
      installButton.setText("");
      return;
    }

    PlayerInfo currentPlayer = playerService.getCurrentPlayer();
    reviewsController.setCanWriteReview(modService.isInstalled(newValue.uid()) && !currentPlayer.getUsername()
                                                                                                .equals(newValue.mod()
                                                                                                                .author()) && !currentPlayer.equals(
        newValue.mod().uploader()));

    reviewService.getModReviews(newValue.mod())
        .collectList()
        .publishOn(fxApplicationThreadExecutor.asScheduler())
        .subscribe(modReviews::setAll, throwable -> log.error("Unable to populate reviews", throwable));

    modService.getFileSize(newValue)
        .thenAcceptAsync(modFileSize -> {
          if (modFileSize > -1) {
            String size = Bytes.formatSize(modFileSize, i18n.getUserSpecificLocale());
            installButton.setText(i18n.get("modVault.install", size));
            sizeLabel.setText(size);
          } else {
            installButton.setText(i18n.get("modVault.install"));
            sizeLabel.setText(i18n.get("notAvailable"));
          }
        }, fxApplicationThreadExecutor);
  }

  private void initializeReviewsController() {
    reviewsController.setCanWriteReview(false);
    reviewsController.setOnSendReviewListener(this::onSendReview);
    reviewsController.setOnDeleteReviewListener(this::onDeleteReview);
    reviewsController.setReviewSupplier(
        () -> new ModVersionReview(null, null, playerService.getCurrentPlayer(), 0, modVersion.get()));
    reviewsController.bindReviews(modReviews);
  }

  @VisibleForTesting
  void onDeleteReview(ModVersionReview review) {
    reviewService.deleteReview(review)
        .publishOn(fxApplicationThreadExecutor.asScheduler())
        .subscribe(null, throwable -> {
          log.error("Review could not be deleted", throwable);
          notificationService.addImmediateErrorNotification(throwable, "review.delete.error");
        }, () -> modReviews.remove(review));
  }

  @VisibleForTesting
  void onSendReview(ModVersionReview review) {
    reviewService.saveReview(review)
        .filter(savedReview -> !modReviews.contains(savedReview))
        .publishOn(fxApplicationThreadExecutor.asScheduler())
        .subscribe(savedReview -> {
          modReviews.remove(review);
          modReviews.add(savedReview);
        }, throwable -> {
          log.error("Review could not be saved", throwable);
          notificationService.addImmediateErrorNotification(throwable, "review.save.error");
        });
  }

  public void onInstallButtonClicked() {
    ModVersion modVersion = this.modVersion.get();
    modService.downloadIfNecessary(modVersion, null, null).subscribe(null, throwable -> {
          log.error("Could not install mod", throwable);
          notificationService.addImmediateErrorNotification(throwable, "modVault.installationFailed",
                                                            modVersion.mod().displayName(),
                                                            throwable.getLocalizedMessage());
        });
  }

  public void onUninstallButtonClicked() {
    ModVersion modVersion = this.modVersion.get();
    modService.uninstallMod(modVersion)
        .exceptionally(throwable -> {
          log.error("Could not delete mod", throwable);
          notificationService.addImmediateErrorNotification(throwable, "modVault.couldNotDeleteMod",
                                                            modVersion.mod().displayName(),
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
}
