package com.faforever.client.mod;

import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionReviewBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.FileSizeReader;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.commons.io.Bytes;
import com.google.common.annotations.VisibleForTesting;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class ModDetailController implements Controller<Node> {

  private final ModService modService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ReportingService reportingService;
  private final TimeService timeService;
  private final ReviewService reviewService;
  private final PlayerService playerService;
  private final UiService uiService;

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

  private ModVersionBean modVersion;
  private ListChangeListener<ModVersionBean> installStatusChangeListener;

  public void initialize() {
    JavaFxUtil.bindManagedToVisible(uninstallButton, installButton, progressBar, progressLabel, getRoot());
    JavaFxUtil.addLabelContextMenus(uiService, nameLabel, authorLabel, idLabel, uploaderLabel, versionLabel);
    JavaFxUtil.fixScrollSpeed(scrollPane);
    progressBar.visibleProperty().bind(uninstallButton.visibleProperty().not().and(installButton.visibleProperty().not()));
    progressLabel.visibleProperty().bind(progressBar.visibleProperty());


    modDetailRoot.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ESCAPE) {
        onCloseButtonClicked();
      }
    });

    installStatusChangeListener = change -> {
      while (change.next()) {
        for (ModVersionBean modVersion : change.getAddedSubList()) {
          if (this.modVersion.equals(modVersion)) {
            setInstalled(true);
            return;
          }
        }
        for (ModVersionBean modVersion : change.getRemoved()) {
          if (this.modVersion.equals(modVersion)) {
            setInstalled(false);
            return;
          }
        }
      }
    };

    // TODO hidden until dependencies are available
    dependenciesTitle.setManaged(false);
    dependenciesContainer.setManaged(false);

    reviewsController.setCanWriteReview(false);
    reviewsController.setReviewSupplier(ModVersionReviewBean::new);
  }

  public void onCloseButtonClicked() {
    getRoot().setVisible(false);
  }

  private void setInstalled(boolean installed) {
    installButton.setVisible(!installed);
    uninstallButton.setVisible(installed);
  }

  public Node getRoot() {
    return modDetailRoot;
  }

  public void setModVersion(ModVersionBean modVersion) {
    this.modVersion = modVersion;
    thumbnailImageView.setImage(modService.loadThumbnail(modVersion));
    nameLabel.setText(modVersion.getMod().getDisplayName());
    idLabel.setText(i18n.get("mod.idNumber", modVersion.getId()));

    setUploaderAndAuthor(modVersion);

    boolean modInstalled = modService.isModInstalled(modVersion.getUid());
    installButton.setVisible(!modInstalled);
    uninstallButton.setVisible(modInstalled);

    modDescriptionLabel.setText(modVersion.getDescription());
    JavaFxUtil.addListener(modService.getInstalledModVersions(), new WeakListChangeListener<>(installStatusChangeListener));
    setInstalled(modService.isModInstalled(modVersion.getUid()));

    updatedLabel.setText(timeService.asDate(modVersion.getUpdateTime()));
    modService.getFileSize(modVersion)
        .thenAccept(modFileSize -> JavaFxUtil.runLater(() -> {
          if (modFileSize > -1) {
            String size = Bytes.formatSize(modFileSize, i18n.getUserSpecificLocale());
            installButton.setText(i18n.get("modVault.installButtonFormat", size));
            installButton.setDisable(false);
            sizeLabel.setText(size);
          } else {
            installButton.setText(i18n.get("notAvailable"));
            installButton.setDisable(true);
            sizeLabel.setText(i18n.get("notAvailable"));
          }
        }));

    versionLabel.setText(modVersion.getVersion().toString());

    PlayerBean player = playerService.getCurrentPlayer();

    reviewsController.setCanWriteReview(modService.isModInstalled(modVersion.getUid())
        && !player.getUsername().equals(modVersion.getMod().getAuthor()) && !player.equals(modVersion.getMod().getUploader()));
    reviewsController.setOnSendReviewListener(this::onSendReview);
    reviewsController.setOnDeleteReviewListener(this::onDeleteReview);
    reviewsController.setReviews(modVersion.getMod().getVersions().stream()
        .flatMap(version -> version.getReviews().stream())
        .collect(Collectors.toCollection(FXCollections::observableArrayList)));
    reviewsController.setOwnReview(modVersion.getReviews().stream()
        .filter(review -> review.getPlayer().getId().equals(player.getId()))
        .findFirst().orElse(null));
  }

  private void setUploaderAndAuthor(ModVersionBean modVersion) {
    if (modVersion.getMod() != null) {
      PlayerBean uploader = modVersion.getMod().getUploader();

      if (uploader != null) {
        uploaderLabel.setText(i18n.get("modVault.details.uploader", uploader.getUsername()));
      } else {
        uploaderLabel.setText(null);
      }
      authorLabel.setText(i18n.get("modVault.details.author", modVersion.getMod().getAuthor()));
    }
  }

  @VisibleForTesting
  void onDeleteReview(ModVersionReviewBean review) {
    reviewService.deleteModVersionReview(review)
        .thenRun(() -> JavaFxUtil.runLater(() -> {
          modVersion.getReviews().remove(review);
          reviewsController.setOwnReview(null);
        }))
        .exceptionally(throwable -> {
          log.warn("Review could not be deleted", throwable);
          notificationService.addImmediateErrorNotification(throwable, "review.delete.error");
          return null;
        });
  }

  @VisibleForTesting
  void onSendReview(ModVersionReviewBean review) {
    boolean isNew = review.getId() == null;
    PlayerBean player = playerService.getCurrentPlayer();
    review.setPlayer(player);
    review.setModVersion(modVersion);
    reviewService.saveModVersionReview(review)
        .thenRun(() -> {
          if (isNew) {
            modVersion.getReviews().add(review);
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
    modService.downloadAndInstallMod(modVersion, progressBar.progressProperty(), progressLabel.textProperty())
        .thenRun(() -> setInstalled(true))
        .exceptionally(throwable -> {
          log.error("Could not install mod", throwable);
          notificationService.addImmediateErrorNotification(throwable, "modVault.installationFailed",
              modVersion.getMod().getDisplayName(), throwable.getLocalizedMessage());
          setInstalled(false);
          return null;
        });
  }

  public void onUninstallButtonClicked() {
    progressBar.progressProperty().unbind();
    progressBar.setProgress(-1);

    modService.uninstallMod(modVersion).thenRun(() -> setInstalled(false))
        .exceptionally(throwable -> {
          log.error("Could not delete mod", throwable);
          notificationService.addImmediateErrorNotification(throwable, "modVault.couldNotDeleteMod",
              modVersion.getMod().getDisplayName(), throwable.getLocalizedMessage());
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
}
