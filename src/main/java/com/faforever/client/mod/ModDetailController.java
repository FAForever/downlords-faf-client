package com.faforever.client.mod;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.events.ImmediateErrorNotificationEvent;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.commons.io.Bytes;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
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

import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class ModDetailController implements Controller<Node> {

  private final ModService modService;
  private final I18n i18n;
  private final ReportingService reportingService;
  private final TimeService timeService;
  private final ReviewService reviewService;
  private final PlayerService playerService;
  private final UiService uiService;
  private final EventBus eventBus;

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
  public ReviewsController reviewsController;
  public Label authorLabel;

  private ModVersion modVersion;
  private ListChangeListener<ModVersion> installStatusChangeListener;

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
        for (ModVersion modVersion : change.getAddedSubList()) {
          if (this.modVersion.equals(modVersion)) {
            setInstalled(true);
            return;
          }
        }
        for (ModVersion modVersion : change.getRemoved()) {
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

  public void setModVersion(ModVersion modVersion) {
    this.modVersion = modVersion;
    thumbnailImageView.setImage(modService.loadThumbnail(modVersion));
    nameLabel.setText(modVersion.getDisplayName());
    idLabel.setText(i18n.get("mod.idNumber", modVersion.getId()));

    setUploaderAndAuthor(modVersion);

    boolean modInstalled = modService.isModInstalled(modVersion.getUid());
    installButton.setVisible(!modInstalled);
    uninstallButton.setVisible(modInstalled);

    modDescriptionLabel.setText(modVersion.getDescription());
    JavaFxUtil.addListener(modService.getInstalledModVersions(), new WeakListChangeListener<>(installStatusChangeListener));
    setInstalled(modService.isModInstalled(modVersion.getUid()));

    updatedLabel.setText(timeService.asDate(modVersion.getUpdateTime()));
    sizeLabel.setText(Bytes.formatSize(getModSize(), i18n.getUserSpecificLocale()));
    versionLabel.setText(modVersion.getVersion().toString());

    Player player = playerService.getCurrentPlayer()
        .orElseThrow(() -> new IllegalStateException("No current player is available"));

    reviewsController.setCanWriteReview(modService.isModInstalled(modVersion.getUid())
        && !modVersion.getMod().getAuthor().equals(player.getUsername()));
    reviewsController.setOnSendReviewListener(this::onSendReview);
    reviewsController.setOnDeleteReviewListener(this::onDeleteReview);
    reviewsController.setReviews(modVersion.getReviews());
    reviewsController.setOwnReview(modVersion.getReviews().stream()
        .filter(review -> review.getPlayer().getId() == player.getId())
        .findFirst());
  }

  private void setUploaderAndAuthor(ModVersion modVersion) {
    if (modVersion.getMod() != null) {
      String uploader = modVersion.getMod().getUploader();

      if (uploader != null) {
        uploaderLabel.setText(i18n.get("modVault.details.uploader", uploader));
      } else {
        uploaderLabel.setText(null);
      }
      authorLabel.setText(i18n.get("modVault.details.author", modVersion.getMod().getAuthor()));
    }
  }

  @VisibleForTesting
  void onDeleteReview(Review review) {
    reviewService.deleteModVersionReview(review)
        .thenRun(() -> Platform.runLater(() -> {
          modVersion.getReviews().remove(review);
          reviewsController.setOwnReview(Optional.empty());
        }))
        .exceptionally(throwable -> {
          log.warn("Review could not be deleted", throwable);
          eventBus.post(new ImmediateErrorNotificationEvent(throwable, "review.delete.error"));
          return null;
        });
  }

  @VisibleForTesting
  void onSendReview(Review review) {
    boolean isNew = review.getId() == null;
    Player player = playerService.getCurrentPlayer()
        .orElseThrow(() -> new IllegalStateException("No current player is available"));
    review.setPlayer(player);
    reviewService.saveModVersionReview(review, modVersion.getId())
        .thenRun(() -> {
          if (isNew) {
            modVersion.getReviews().add(review);
          }
          reviewsController.setOwnReview(Optional.of(review));
        })
        .exceptionally(throwable -> {
          log.warn("Review could not be saved", throwable);
          eventBus.post(new ImmediateErrorNotificationEvent(throwable, "review.save.error"));
          return null;
        });
  }

  private long getModSize() {
    return modService.getModSize(modVersion);
  }

  public void onInstallButtonClicked() {
    modService.downloadAndInstallMod(modVersion, progressBar.progressProperty(), progressLabel.textProperty())
        .thenRun(() -> setInstalled(true))
        .exceptionally(throwable -> {
          log.error("Could not install mod", throwable);
          eventBus.post(new ImmediateErrorNotificationEvent(throwable, "modVault.installationFailed",
              modVersion.getDisplayName(), throwable.getLocalizedMessage()));
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
          eventBus.post(new ImmediateErrorNotificationEvent(throwable, "modVault.couldNotDeleteMod",
              modVersion.getDisplayName(), throwable.getLocalizedMessage()));
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
