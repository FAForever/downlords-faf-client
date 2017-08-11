package com.faforever.client.mod;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.StarsController;
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
  private Mod mod;
  private Consumer<Mod> onOpenDetailListener;
  private ListChangeListener<Mod> installStatusChangeListener;
  public StarsController starsController;
  private InvalidationListener reviewsChangedListener;


  @Inject
  public ModCardController(ModService modService, TimeService timeService, I18n i18n) {
    this.modService = modService;
    this.timeService = timeService;
    this.i18n = i18n;
    reviewsChangedListener = observable -> populateReviews();
  }

  private void populateReviews() {
    ObservableList<Review> reviews = mod.getReviews();
    Platform.runLater(() -> {
      numberOfReviewsLabel.setText(i18n.number(reviews.size()));
      starsController.setValue((float) reviews.stream().mapToInt(Review::getScore).average().orElse(0d));
    });
  }

  public void initialize() {
    installStatusChangeListener = change -> {
      while (change.next()) {
        for (Mod mod : change.getAddedSubList()) {
          if (this.mod.equals(mod)) {
            setInstalled(true);
            return;
          }
        }
        for (Mod mod : change.getRemoved()) {
          if (this.mod.equals(mod)) {
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

  public void setMod(Mod mod) {
    this.mod = mod;
    thumbnailImageView.setImage(modService.loadThumbnail(mod));
    nameLabel.setText(mod.getDisplayName());
    authorLabel.setText(mod.getUploader());
    createdLabel.setText(timeService.asDate(mod.getCreateTime()));
    typeLabel.setText(mod.getModType() != null ? i18n.get(mod.getModType().getI18nKey()) : "");

    ObservableList<Mod> installedMods = modService.getInstalledMods();
    synchronized (installedMods) {
      installedMods.addListener(new WeakListChangeListener<>(installStatusChangeListener));
    }
    ObservableList<Review> reviews = mod.getReviews();
    reviews.addListener(new WeakInvalidationListener(reviewsChangedListener));
    reviewsChangedListener.invalidated(reviews);
  }

  public Node getRoot() {
    return modTileRoot;
  }

  public void setOnOpenDetailListener(Consumer<Mod> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowModDetail() {
    onOpenDetailListener.accept(mod);
  }
}
