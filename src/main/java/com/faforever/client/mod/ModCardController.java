package com.faforever.client.mod;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
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
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ModCardController implements Controller<Node> {

  private final ModService modService;
  private final TimeService timeService;
  private final I18n i18n;
  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label authorLabel;
  public Node modTileRoot;
  public Label createdLabel;
  public Label numberOfReviewsLabel;
  public Label typeLabel;
  private ModVersion modVersion;
  private Consumer<ModVersion> onOpenDetailListener;
  private ListChangeListener<ModVersion> installStatusChangeListener;
  public StarsController starsController;
  private InvalidationListener reviewsChangedListener;
  private JFXRippler jfxRippler;


  @Inject
  public ModCardController(ModService modService, TimeService timeService, I18n i18n) {
    this.modService = modService;
    this.timeService = timeService;
    this.i18n = i18n;
    reviewsChangedListener = observable -> populateReviews();
  }

  private void populateReviews() {
    ObservableList<Review> reviews = modVersion.getReviews();
    Platform.runLater(() -> {
      numberOfReviewsLabel.setText(i18n.number(reviews.size()));
      starsController.setValue((float) reviews.stream().mapToInt(Review::getScore).average().orElse(0d));
    });
  }

  public void initialize() {
    jfxRippler = new JFXRippler(modTileRoot);
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

  private void setInstalled(boolean installed) {
    //TODO:IMPLEMENT ISSUE #670
  }

  public void setModVersion(ModVersion modVersion) {
    this.modVersion = modVersion;
    thumbnailImageView.setImage(modService.loadThumbnail(modVersion));
    nameLabel.setText(modVersion.getDisplayName());
    authorLabel.setText(modVersion.getUploader());
    createdLabel.setText(timeService.asDate(modVersion.getCreateTime()));
    typeLabel.setText(modVersion.getModType() != null ? i18n.get(modVersion.getModType().getI18nKey()) : "");

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
