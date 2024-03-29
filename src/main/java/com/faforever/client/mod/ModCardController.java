package com.faforever.client.mod;

import com.faforever.client.domain.api.Mod;
import com.faforever.client.domain.api.ModType;
import com.faforever.client.domain.api.ModVersion;
import com.faforever.client.domain.api.ReviewsSummary;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.VaultEntityCardController;
import com.faforever.client.vault.review.StarsController;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
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
public class ModCardController extends VaultEntityCardController<ModVersion> {

  private final UiService uiService;
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
  public StarsController starsController;

  private Consumer<ModVersion> onOpenDetailListener;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(installButton, uninstallButton);
    ObservableValue<Mod> modObservable = entity.map(ModVersion::mod);
    numberOfReviewsLabel.textProperty().bind(modObservable.map(Mod::reviewsSummary).map(ReviewsSummary::numReviews)
                                           .orElse(0)
                                           .map(i18n::number)
                                           .when(showing));
    starsController.valueProperty().bind(modObservable.map(Mod::reviewsSummary)
                           .map(reviewsSummary -> reviewsSummary.score() / reviewsSummary.numReviews())
            .when(showing));

    BooleanExpression isModInstalled = modService.isInstalledBinding(entity);

    installButton.visibleProperty().bind(isModInstalled.not().when(showing));
    uninstallButton.visibleProperty().bind(isModInstalled.when(showing));

    thumbnailImageView.imageProperty()
        .bind(entity.map(modService::loadThumbnail)
            .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
            .when(showing));

    nameLabel.textProperty().bind(modObservable.map(Mod::displayName).when(showing));
    authorLabel.textProperty().bind(modObservable.map(Mod::author).when(showing));
    typeLabel.textProperty()
             .bind(entity.map(ModVersion::modType).map(ModType::getI18nKey).map(i18n::get).when(showing));
  }

  public void onInstallButtonClicked() {
    ModVersion modVersion = entity.get();
    modService.downloadIfNecessary(modVersion, null, null).subscribe(null, throwable -> {
      log.error("Could not install mod", throwable);
      notificationService.addImmediateErrorNotification(throwable, "modVault.installationFailed",
                                                        modVersion.mod().displayName(),
                                                        throwable.getLocalizedMessage());
    });
  }

  public void onUninstallButtonClicked() {
    ModVersion modVersion = entity.get();
    modService.uninstallMod(modVersion).exceptionally(throwable -> {
      log.error("Could not delete mod", throwable);
      notificationService.addImmediateErrorNotification(throwable, "modVault.couldNotDeleteMod",
                                                        modVersion.mod().displayName(),
                                                        throwable.getLocalizedMessage());
      return null;
    });
  }

  @Override
  public Node getRoot() {
    return modTileRoot;
  }

  public void setOnOpenDetailListener(Consumer<ModVersion> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowModDetail() {
    onOpenDetailListener.accept(entity.get());
  }
}
