package com.faforever.client.mod;

import com.faforever.client.domain.ModBean;
import com.faforever.client.domain.ModReviewsSummaryBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionBean.ModType;
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
public class ModCardController extends VaultEntityCardController<ModVersionBean> {

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

  private Consumer<ModVersionBean> onOpenDetailListener;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(installButton, uninstallButton);
    ObservableValue<ModBean> modObservable = entity.flatMap(ModVersionBean::modProperty);
    numberOfReviewsLabel.textProperty()
        .bind(modObservable.flatMap(ModBean::modReviewsSummaryProperty)
            .flatMap(ModReviewsSummaryBean::numReviewsProperty)
            .orElse(0)
            .map(i18n::number)
            .when(showing));
    starsController.valueProperty()
        .bind(modObservable.flatMap(ModBean::modReviewsSummaryProperty)
            .flatMap(reviewsSummary -> reviewsSummary.scoreProperty().divide(reviewsSummary.numReviewsProperty()))
            .when(showing));

    BooleanExpression isModInstalled = modService.isInstalledBinding(entity);

    installButton.visibleProperty().bind(isModInstalled.not().when(showing));
    uninstallButton.visibleProperty().bind(isModInstalled.when(showing));

    thumbnailImageView.imageProperty()
        .bind(entity.map(modService::loadThumbnail)
            .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
            .when(showing));

    nameLabel.textProperty().bind(modObservable.flatMap(ModBean::displayNameProperty).when(showing));
    authorLabel.textProperty().bind(modObservable.flatMap(ModBean::authorProperty).when(showing));
    typeLabel.textProperty()
        .bind(entity.flatMap(ModVersionBean::modTypeProperty).map(ModType::getI18nKey).map(i18n::get).when(showing));
  }

  public void onInstallButtonClicked() {
    ModVersionBean modVersionBean = entity.get();
    modService.downloadAndInstallMod(modVersionBean, null, null).exceptionally(throwable -> {
      log.error("Could not install mod", throwable);
      notificationService.addImmediateErrorNotification(throwable, "modVault.installationFailed", modVersionBean.getMod()
          .getDisplayName(), throwable.getLocalizedMessage());
      return null;
    });
  }

  public void onUninstallButtonClicked() {
    ModVersionBean modVersionBean = entity.get();
    modService.uninstallMod(modVersionBean).exceptionally(throwable -> {
      log.error("Could not delete mod", throwable);
      notificationService.addImmediateErrorNotification(throwable, "modVault.couldNotDeleteMod", modVersionBean.getMod()
          .getDisplayName(), throwable.getLocalizedMessage());
      return null;
    });
  }

  @Override
  public Node getRoot() {
    return modTileRoot;
  }

  public void setOnOpenDetailListener(Consumer<ModVersionBean> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowModDetail() {
    onOpenDetailListener.accept(entity.get());
  }
}
