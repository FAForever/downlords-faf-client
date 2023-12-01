package com.faforever.client.mod;

import com.faforever.client.domain.ModBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionReviewBean;
import com.faforever.client.domain.PlayerBean;
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
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.commons.io.Bytes;
import com.google.common.annotations.VisibleForTesting;
import io.micrometer.common.util.StringUtils;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
  private final UiService uiService;
  private final ContextMenuBuilder contextMenuBuilder;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final ObjectProperty<ModVersionBean> modVersion = new SimpleObjectProperty<>();
  private final ObservableList<ModVersionReviewBean> modReviews = FXCollections.observableArrayList();

  public Label updatedLabel;
  public Label sizeLabel;
  public Label versionLabel;
  public Label dependenciesTitle;
  public VBox dependenciesContainer;
  public Label progressLabel;
  public ScrollPane scrollPane;
  public Button uninstallButton;
  public Button installButton;
  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label idLabel;
  public Label uploaderLabel;
  public ProgressBar progressBar;
  public Label modDescriptionLabel;
  public Node modDetailRoot;
  public ReviewsController<ModVersionReviewBean> reviewsController;
  public Label authorLabel;

  @Override
  protected void onInitialize() {
    imageViewHelper.setDefaultPlaceholderImage(thumbnailImageView);
    JavaFxUtil.bindManagedToVisible(uninstallButton, installButton, progressBar, progressLabel, getRoot());
    JavaFxUtil.fixScrollSpeed(scrollPane);

    contextMenuBuilder.addCopyLabelContextMenu(nameLabel, authorLabel, idLabel, uploaderLabel, versionLabel);
    modDetailRoot.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ESCAPE) {
        onCloseButtonClicked();
      }
    });

    initializeReviewsController();
    bindProperties();
    modVersion.addListener((SimpleChangeListener<ModVersionBean>) this::onModVersionChanged);

    // TODO hidden until dependencies are available
    dependenciesTitle.setManaged(false);
    dependenciesContainer.setManaged(false);
  }

  private void bindProperties() {
    ObservableValue<ModBean> modObservable = modVersion.flatMap(ModVersionBean::modProperty);
    thumbnailImageView.imageProperty()
        .bind(modVersion.map(modService::loadThumbnail)
            .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
            .when(showing));
    nameLabel.textProperty().bind(modObservable.flatMap(ModBean::displayNameProperty).when(showing));
    authorLabel.textProperty()
        .bind(modObservable.flatMap(ModBean::authorProperty)
            .map(author -> i18n.get("modVault.details.author", author))
            .when(showing));
    uploaderLabel.textProperty()
        .bind(modObservable.flatMap(ModBean::uploaderProperty)
            .flatMap(PlayerBean::usernameProperty)
            .map(author -> i18n.get("modVault.details.uploader", author))
            .when(showing));
    idLabel.textProperty()
        .bind(modVersion.flatMap(ModVersionBean::idProperty).map(id -> i18n.get("mod.idNumber", id)).when(showing));

    updatedLabel.textProperty()
        .bind(modVersion.flatMap(ModVersionBean::createTimeProperty).map(timeService::asDate).when(showing));

    modDescriptionLabel.textProperty().bind(modVersion.flatMap(ModVersionBean::descriptionProperty)
        .map(FaStrings::removeLocalizationTag)
        .map(description -> StringUtils.isBlank(description) ? i18n.get("map.noDescriptionAvailable") : description)
        .when(showing));

    versionLabel.textProperty()
        .bind(modVersion.flatMap(ModVersionBean::versionProperty).map(ComparableVersion::toString).when(showing));

    BooleanExpression installed = modService.isInstalledBinding(modVersion);
    installButton.visibleProperty().bind(installed.not().when(showing));
    uninstallButton.visibleProperty().bind(installed.when(showing));
    progressBar.visibleProperty()
               .bind(uninstallButton.visibleProperty().not().and(installButton.visibleProperty().not()).when(showing));
    progressLabel.visibleProperty().bind(progressBar.visibleProperty().when(showing));
  }

  public void onCloseButtonClicked() {
    getRoot().setVisible(false);
  }

  @Override
  public Node getRoot() {
    return modDetailRoot;
  }

  public void setModVersion(ModVersionBean modVersion) {
    this.modVersion.set(modVersion);
  }

  private void onModVersionChanged(ModVersionBean newValue) {
    if (newValue == null) {
      reviewsController.setCanWriteReview(false);
      modReviews.clear();
      installButton.setText("");
      return;
    }

    PlayerBean currentPlayer = playerService.getCurrentPlayer();
    reviewsController.setCanWriteReview(modService.isInstalled(newValue.getUid())
        && !currentPlayer.getUsername()
        .equals(newValue.getMod().getAuthor()) && !currentPlayer.equals(newValue
        .getMod()
        .getUploader()));

    reviewService.getModReviews(newValue.getMod())
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
    reviewsController.setReviewSupplier(() -> {
      ModVersionReviewBean review = new ModVersionReviewBean();
      review.setPlayer(playerService.getCurrentPlayer());
      review.setModVersion(modVersion.get());
      return review;
    });
    reviewsController.bindReviews(modReviews);
  }

  @VisibleForTesting
  void onDeleteReview(ModVersionReviewBean review) {
    reviewService.deleteModVersionReview(review)
        .publishOn(fxApplicationThreadExecutor.asScheduler())
        .subscribe(null, throwable -> {
          log.error("Review could not be deleted", throwable);
          notificationService.addImmediateErrorNotification(throwable, "review.delete.error");
        }, () -> modReviews.remove(review));
  }

  @VisibleForTesting
  void onSendReview(ModVersionReviewBean review) {
    reviewService.saveModVersionReview(review)
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
    ModVersionBean modVersion = this.modVersion.get();
    modService.downloadAndInstallMod(modVersion, progressBar.progressProperty(), progressLabel.textProperty())
        .exceptionally(throwable -> {
          log.error("Could not install mod", throwable);
          notificationService.addImmediateErrorNotification(throwable, "modVault.installationFailed",
              modVersion.getMod().getDisplayName(), throwable.getLocalizedMessage());
          return null;
        });
  }

  public void onUninstallButtonClicked() {
    progressBar.progressProperty().unbind();
    progressBar.setProgress(-1);

    ModVersionBean modVersion = this.modVersion.get();
    modService.uninstallMod(modVersion)
        .exceptionally(throwable -> {
          log.error("Could not delete mod", throwable);
          notificationService.addImmediateErrorNotification(throwable, "modVault.couldNotDeleteMod",
              modVersion.getMod().getDisplayName(), throwable.getLocalizedMessage());
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
