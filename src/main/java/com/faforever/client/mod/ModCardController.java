package com.faforever.client.mod;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.StarsController;
import com.jfoenix.controls.JFXRippler;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ModCardController implements Controller<Node> {

  private final ModService modService;
  private final NotificationService notificationService;
  private final TimeService timeService;
  private final I18n i18n;
  private final ReportingService reportingService;
  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label authorLabel;
  public Node modTileRoot;
  public Label createdLabel;
  public Label updatedLabel;
  public Label numberOfReviewsLabel;
  public Label typeLabel;
  public Button installButton;
  public Button uninstallButton;
  private ModVersion modVersion;
  private Consumer<ModVersion> onOpenDetailListener;
  private ListChangeListener<ModVersion> installStatusChangeListener;
  public StarsController starsController;
  private final InvalidationListener reviewsChangedListener = observable -> populateReviews();
  private JFXRippler jfxRippler;

  private void populateReviews() {
    ObservableList<Review> reviews = modVersion.getReviews();
    Platform.runLater(() -> {
      numberOfReviewsLabel.setText(i18n.number(reviews.size()));
      starsController.setValue((float) reviews.stream().mapToInt(Review::getScore).average().orElse(0d));
    });
  }

  public void initialize() {
    jfxRippler = new JFXRippler(modTileRoot);
    installButton.managedProperty().bind(installButton.visibleProperty());
    uninstallButton.managedProperty().bind(uninstallButton.visibleProperty());
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
  }

  public void onInstallButtonClicked() {
    modService.downloadAndInstallMod(modVersion, null, null)
        .thenRun(() -> setInstalled(true))
        .exceptionally(throwable -> {
          notificationService.addImmediateErrorNotification(throwable, "modVault.installationFailed",
              modVersion.getDisplayName(), throwable.getLocalizedMessage());
          setInstalled(false);
          return null;
        });
  }

  public void onUninstallButtonClicked() {
    modService.uninstallMod(modVersion).thenRun(() -> setInstalled(false))
        .exceptionally(throwable -> {
          notificationService.addImmediateErrorNotification(throwable, "modVault.couldNotDeleteMod",
              modVersion.getDisplayName(), throwable.getLocalizedMessage());
          setInstalled(true);
          return null;
        });
  }

  private void setInstalled(boolean installed) {
    installButton.setVisible(!installed);
    uninstallButton.setVisible(installed);
  }

  public void setModVersion(ModVersion modVersion) {
    this.modVersion = modVersion;
    thumbnailImageView.setImage(modService.loadThumbnail(modVersion));
    nameLabel.setText(modVersion.getDisplayName());
    if (modVersion.getMod() != null) {
      authorLabel.setText(modVersion.getMod().getAuthor());
    }
    createdLabel.setText(timeService.asDate(modVersion.getCreateTime()));
    updatedLabel.setText(timeService.asDate(modVersion.getUpdateTime()));
    typeLabel.setText(modVersion.getModType() != null ? i18n.get(modVersion.getModType().getI18nKey()) : "");
    setInstalled(modService.isModInstalled(modVersion.getUid()));

    ObservableList<ModVersion> installedModVersions = modService.getInstalledModVersions();
    JavaFxUtil.addListener(installedModVersions, new WeakListChangeListener<>(installStatusChangeListener));

    ObservableList<Review> reviews = modVersion.getReviews();
    JavaFxUtil.addListener(reviews, new WeakInvalidationListener(reviewsChangedListener));
    reviewsChangedListener.invalidated(reviews);
  }

  public Node getRoot() {
    return jfxRippler;
  }

  public void setOnOpenDetailListener(Consumer<ModVersion> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowModDetail() {
    onOpenDetailListener.accept(modVersion);
  }
}
