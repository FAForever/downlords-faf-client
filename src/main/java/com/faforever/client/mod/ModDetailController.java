package com.faforever.client.mod;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.commons.io.Bytes;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

import static java.util.Collections.singletonList;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class ModDetailController implements Controller<Node> {

  private final ModService modService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ReportingService reportingService;
  private final TimeService timeService;
  private final ReviewService reviewService;
  private final PlayerService playerService;

  public Label updatedLabel;
  public Label sizeLabel;
  public Label installationsLabel;
  public Label versionLabel;
  public Label dependenciesTitle;
  public VBox dependenciesContainer;
  public Label progressLabel;
  public ScrollPane scrollPane;
  public Button uninstallButton;
  public Button installButton;
  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label authorLabel;
  public ProgressBar progressBar;
  public Label modDescriptionLabel;
  public Node modDetailRoot;
  public ReviewsController reviewsController;

  private Mod mod;
  private ListChangeListener<Mod> installStatusChangeListener;

  @Inject
  public ModDetailController(ModService modService, NotificationService notificationService, I18n i18n,
                             ReportingService reportingService, TimeService timeService, ReviewService reviewService,
                             PlayerService playerService) {
    this.modService = modService;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.reportingService = reportingService;
    this.timeService = timeService;
    this.reviewService = reviewService;
    this.playerService = playerService;
  }

  public void initialize() {
    JavaFxUtil.fixScrollSpeed(scrollPane);
    uninstallButton.managedProperty().bind(uninstallButton.visibleProperty());
    installButton.managedProperty().bind(installButton.visibleProperty());
    progressBar.managedProperty().bind(progressBar.visibleProperty());
    progressBar.visibleProperty().bind(uninstallButton.visibleProperty().not().and(installButton.visibleProperty().not()));
    progressLabel.managedProperty().bind(progressLabel.visibleProperty());
    progressLabel.visibleProperty().bind(progressBar.visibleProperty());

    modDetailRoot.setOnKeyPressed(keyEvent -> {
      if (keyEvent.getCode() == KeyCode.ESCAPE) {
        onCloseButtonClicked();
      }
    });

    installStatusChangeListener = change -> {
      while (change.next()) {
        for (Mod mod : change.getAddedSubList()) {
          if (this.mod.getUid().equals(mod.getUid())) {
            setInstalled(true);
            return;
          }
        }
        for (Mod mod : change.getRemoved()) {
          if (this.mod.getUid().equals(mod.getUid())) {
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
    ((Pane) modDetailRoot.getParent()).getChildren().remove(modDetailRoot);
  }

  private void setInstalled(boolean installed) {
    installButton.setVisible(!installed);
    uninstallButton.setVisible(installed);
  }

  public Node getRoot() {
    return modDetailRoot;
  }

  public void setMod(Mod mod) {
    this.mod = mod;
    thumbnailImageView.setImage(modService.loadThumbnail(mod));
    nameLabel.setText(mod.getName());
    authorLabel.setText(mod.getAuthor());

    boolean modInstalled = modService.isModInstalled(mod.getUid());
    installButton.setVisible(!modInstalled);
    uninstallButton.setVisible(modInstalled);

    modDescriptionLabel.setText(mod.getDescription());
    modService.getInstalledMods().addListener(new WeakListChangeListener<>(installStatusChangeListener));
    setInstalled(modService.isModInstalled(mod.getUid()));

    updatedLabel.setText(timeService.asDate(mod.getPublishDate()));
    sizeLabel.setText(Bytes.formatSize(getModSize(), i18n.getUserSpecificLocale()));
    installationsLabel.setText(String.format(i18n.getUserSpecificLocale(), "%d", mod.getDownloads()));
    versionLabel.setText(mod.getVersion().toString());

    Player player = playerService.getCurrentPlayer()
        .orElseThrow(() -> new IllegalStateException("No current player is available"));

    reviewsController.setCanWriteReview(modService.isModInstalled(mod.getUid()));
    reviewsController.setOnSendReviewListener(this::onSendReview);
    reviewsController.setOnDeleteReviewListener(this::onDeleteReview);
    reviewsController.setReviews(mod.getReviews());
    reviewsController.setOwnReview(mod.getReviews().stream()
        .filter(review -> review.getPlayer().getId() == player.getId())
        .findFirst());
  }

  private void onDeleteReview(Review review) {
    reviewService.deleteModVersionReview(review)
        .thenRun(() -> Platform.runLater(() -> {
          mod.getReviews().remove(review);
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
    reviewService.saveModVersionReview(review, mod.getId())
        .thenRun(() -> {
          if (isNew) {
            mod.getReviews().add(review);
          }
          reviewsController.setOwnReview(Optional.of(review));
        })
        // TODO display error to user
        .exceptionally(throwable -> {
          log.warn("Review could not be saved", throwable);
          return null;
        });
  }

  private long getModSize() {
    return modService.getModSize(mod);
  }

  public void onInstallButtonClicked() {
    installButton.setVisible(false);

    modService.downloadAndInstallMod(mod, progressBar.progressProperty(), progressLabel.textProperty())
        .thenRun(() -> uninstallButton.setVisible(true))
        .exceptionally(throwable -> {
          notificationService.addNotification(new ImmediateNotification(
              i18n.get("errorTitle"),
              i18n.get("modVault.installationFailed", mod.getName(), throwable.getLocalizedMessage()),
              Severity.ERROR, throwable, singletonList(new ReportAction(i18n, reportingService, throwable))));
          return null;
        });
  }

  public void onUninstallButtonClicked() {
    progressBar.progressProperty().unbind();
    progressBar.setProgress(-1);
    uninstallButton.setVisible(false);

    modService.uninstallMod(mod).exceptionally(throwable -> {
      notificationService.addNotification(new ImmediateNotification(
          i18n.get("errorTitle"),
          i18n.get("modVault.couldNotDeleteMod", mod.getName(), throwable.getLocalizedMessage()),
          Severity.ERROR, throwable, singletonList(new ReportAction(i18n, reportingService, throwable))));
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
