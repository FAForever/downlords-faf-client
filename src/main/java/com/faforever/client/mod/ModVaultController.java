package com.faforever.client.mod;

import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionBean.ModType;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenModVaultEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.VaultPrefs;
import com.faforever.client.query.SearchablePropertyMappings;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
import com.faforever.client.vault.VaultEntityCardController;
import com.faforever.client.vault.VaultEntityController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import javafx.scene.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class ModVaultController extends VaultEntityController<ModVersionBean> {

  private final ModService modService;
  private final PlatformService platformService;
  private final VaultPrefs vaultPrefs;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  private ModDetailController modDetailController;
  private Integer recommendedShowRoomPageCount;

  public ModVaultController(ModService modService, I18n i18n,
                            UiService uiService, NotificationService notificationService,
                            ReportingService reportingService,
                            PlatformService platformService, VaultPrefs vaultPrefs,
                            ForgedAlliancePrefs forgedAlliancePrefs,
                            FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    super(uiService, notificationService, i18n, reportingService, vaultPrefs, fxApplicationThreadExecutor);
    this.modService = modService;
    this.platformService = platformService;
    this.vaultPrefs = vaultPrefs;
    this.forgedAlliancePrefs = forgedAlliancePrefs;
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    JavaFxUtil.fixScrollSpeed(scrollPane);

    manageVaultButton.setVisible(true);
    manageVaultButton.setText(i18n.get("modVault.manageMods"));
    modService.getRecommendedModPageCount(TOP_ELEMENT_COUNT)
        .thenAccept(pageCount -> recommendedShowRoomPageCount = pageCount);
  }

  @Override
  protected void onDisplayDetails(ModVersionBean modVersion) {
    fxApplicationThreadExecutor.execute(() -> {
      modDetailController.setModVersion(modVersion);
      modDetailController.getRoot().setVisible(true);
      modDetailController.getRoot().requestFocus();
    });
  }

  @Override
  protected void setSupplier(SearchConfig searchConfig) {
    switch (searchType) {
      case SEARCH ->
          currentSupplier = modService.findByQueryWithPageCount(searchConfig, pageSize, pagination.getCurrentPageIndex() + 1);
      case NEWEST ->
          currentSupplier = modService.getNewestModsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case HIGHEST_RATED ->
          currentSupplier = modService.getHighestRatedModsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case HIGHEST_RATED_UI ->
          currentSupplier = modService.getHighestRatedUiModsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case RECOMMENDED ->
          currentSupplier = modService.getRecommendedModsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
    }
  }

  @Override
  protected VaultEntityCardController<ModVersionBean> createEntityCard() {
    ModCardController controller = uiService.loadFxml("theme/vault/mod/mod_card.fxml");
    controller.setOnOpenDetailListener(this::onDisplayDetails);
    return controller;
  }

  @Override
  protected List<ShowRoomCategory> getShowRoomCategories() {
    return List.of(
        new ShowRoomCategory(() -> {
          int recommendedPage;
          if (recommendedShowRoomPageCount != null && recommendedShowRoomPageCount > 0) {
            recommendedPage = new Random().nextInt(recommendedShowRoomPageCount) + 1;
          } else {
            recommendedPage = 1;
          }
          return modService.getRecommendedModsWithPageCount(TOP_ELEMENT_COUNT, recommendedPage);
        }, SearchType.RECOMMENDED, "modVault.recommended"),
    new ShowRoomCategory(() -> modService.getHighestRatedModsWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.HIGHEST_RATED, "modVault.highestRated"),
        new ShowRoomCategory(() -> modService.getHighestRatedUiModsWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.HIGHEST_RATED_UI, "modVault.highestRatedUiMods"),
        new ShowRoomCategory(() -> modService.getNewestModsWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.NEWEST, "modVault.newestMods")
    );
  }

  @Override
  public void onUploadButtonClicked() {
    platformService.askForPath(i18n.get("modVault.upload.chooseDirectory"), forgedAlliancePrefs.getModsDirectory())
        .ifPresent(this::openUploadWindow);
  }

  @Override
  protected void onManageVaultButtonClicked() {
    ModManagerController modManagerController = uiService.loadFxml("theme/mod_manager.fxml");
    Dialog dialog = uiService.showInDialog(vaultRoot, modManagerController.getRoot(), i18n.get("modVault.modManager"));
    dialog.setOnDialogClosed(event -> modManagerController.apply());
    modManagerController.setOnCloseButtonClickedListener(dialog::close);
  }

  @Override
  protected Node getDetailView() {
    modDetailController = uiService.loadFxml("theme/vault/mod/mod_detail.fxml");
    return modDetailController.getRoot();
  }

  @Override
  protected void initSearchController() {
    searchController.setRootType(com.faforever.commons.api.dto.Mod.class);
    searchController.setSearchableProperties(SearchablePropertyMappings.MOD_PROPERTY_MAPPING);
    searchController.setSortConfig(vaultPrefs.mapSortConfigProperty());
    searchController.setOnlyShowLastYearCheckBoxVisible(false);
    searchController.setVaultRoot(vaultRoot);
    searchController.setSavedQueries(vaultPrefs.getSavedModQueries());

    searchController.addTextFilter("displayName", i18n.get("mod.displayName"), false);
    searchController.addTextFilter("author", i18n.get("mod.author"), false);
    searchController.addDateRangeFilter("latestVersion.updateTime", i18n.get("mod.uploadedDateTime"), 0);

    searchController.addRangeFilter("reviewsSummary.averageScore", i18n.get("reviews.averageScore"), 0, 5, 10, 4, 1);

    searchController.addBinaryFilter("latestVersion.type", i18n.get("mod.type"),
        ModType.UI.toString(), ModType.SIM.toString(), i18n.get("modType.ui"), i18n.get("modType.sim"));
    searchController.addToggleFilter("latestVersion.ranked", i18n.get("mod.onlyRanked"), "true");
  }

  @Override
  protected Class<? extends NavigateEvent> getDefaultNavigateEvent() {
    return OpenModVaultEvent.class;
  }

  @Override
  protected void handleSpecialNavigateEvent(NavigateEvent navigateEvent) {
    log.warn("No such NavigateEvent for this Controller: {}", navigateEvent.getClass());
  }

  private void openUploadWindow(Path path) {
    ModUploadController modUploadController = uiService.loadFxml("theme/vault/mod/mod_upload.fxml");
    modUploadController.setModPath(path);

    Node root = modUploadController.getRoot();
    Dialog dialog = uiService.showInDialog(vaultRoot, root, i18n.get("modVault.upload.title"));
    modUploadController.setOnCancelButtonClickedListener(dialog::close);
    modUploadController.setUploadListener(this::onRefreshButtonClicked);
  }
}
