package com.faforever.client.mod;

import com.faforever.client.domain.ModReviewsSummaryBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionReviewBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.SimpleInvalidationListener;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.vault.review.StarsController;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class ModCardController implements Controller<Node> {

  private final ModService modService;
  private final ImageViewHelper imageViewHelper;
  private final NotificationService notificationService;
  private final I18n i18n;

  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label authorLabel;
  public Node modTileRoot;
  public Label numberOfReviewsLabel;
  public Label typeLabel;
  public Button installButton;
  public Button uninstallButton;
  private ModVersionBean modVersion;
  private Consumer<ModVersionBean> onOpenDetailListener;
  private ListChangeListener<ModVersionBean> installStatusChangeListener;
  public StarsController starsController;
  private final SimpleInvalidationListener reviewsChangedListener = this::populateReviews;

  private void populateReviews() {
    ModReviewsSummaryBean modReviewsSummary = modVersion.getMod().getModReviewsSummary();
    int numReviews;
    float avgScore;
    if (modReviewsSummary == null) {
      numReviews = 0;
      avgScore = 0;
    } else {
      numReviews = modReviewsSummary.getNumReviews();
      avgScore = modReviewsSummary.getScore() / numReviews;
    }
    JavaFxUtil.runLater(() -> {
      numberOfReviewsLabel.setText(i18n.number(numReviews));
      starsController.setValue(avgScore);
    });
  }

  public void initialize() {
    imageViewHelper.setDefaultPlaceholderImage(thumbnailImageView);
    installButton.managedProperty().bind(installButton.visibleProperty());
    uninstallButton.managedProperty().bind(uninstallButton.visibleProperty());
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
  }

  public void onInstallButtonClicked() {
    modService.downloadAndInstallMod(modVersion, null, null)
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
    modService.uninstallMod(modVersion).thenRun(() -> setInstalled(false))
        .exceptionally(throwable -> {
          log.error("Could not delete mod", throwable);
          notificationService.addImmediateErrorNotification(throwable, "modVault.couldNotDeleteMod",
              modVersion.getMod().getDisplayName(), throwable.getLocalizedMessage());
          setInstalled(true);
          return null;
        });
  }

  private void setInstalled(boolean installed) {
    installButton.setVisible(!installed);
    uninstallButton.setVisible(installed);
  }

  public void setModVersion(ModVersionBean modVersion) {
    this.modVersion = modVersion;
    thumbnailImageView.setImage(modService.loadThumbnail(modVersion));
    nameLabel.setText(modVersion.getMod().getDisplayName());
    if (modVersion.getMod() != null) {
      authorLabel.setText(modVersion.getMod().getAuthor());
    }
    typeLabel.setText(modVersion.getModType() != null ? i18n.get(modVersion.getModType().getI18nKey()) : "");
    setInstalled(modService.isModInstalled(modVersion.getUid()));

    ObservableList<ModVersionBean> installedModVersions = modService.getInstalledModVersions();
    JavaFxUtil.addListener(installedModVersions, new WeakListChangeListener<>(installStatusChangeListener));

    ObservableList<ModVersionReviewBean> reviews = modVersion.getReviews();
    JavaFxUtil.addListener(reviews, new WeakInvalidationListener(reviewsChangedListener));
    reviewsChangedListener.invalidated(reviews);
  }

  public Node getRoot() {
    return modTileRoot;
  }

  public void setOnOpenDetailListener(Consumer<ModVersionBean> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowModDetail() {
    onOpenDetailListener.accept(modVersion);
  }
}
