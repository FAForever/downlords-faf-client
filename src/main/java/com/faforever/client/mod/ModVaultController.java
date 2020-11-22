package com.faforever.client.mod;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenModVaultEvent;
import com.faforever.client.mod.ModVersion.ModType;
import com.faforever.client.mod.event.ModUploadedEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.SearchablePropertyMappings;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.dialog.Dialog;
import com.faforever.client.vault.VaultEntityController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.stage.DirectoryChooser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class ModVaultController extends VaultEntityController<ModVersion> {

  private final ModService modService;
  private final EventBus eventBus;

  private ModDetailController modDetailController;

  public ModVaultController(ModService modService, I18n i18n, EventBus eventBus, PreferencesService preferencesService,
                               UiService uiService, NotificationService notificationService, ReportingService reportingService) {
    super(uiService, notificationService, i18n, preferencesService, reportingService);
    this.eventBus = eventBus;
    this.modService = modService;
  }

  @Override
  public void initialize() {
    super.initialize();
    JavaFxUtil.fixScrollSpeed(scrollPane);

    eventBus.register(this);

    manageModsButton.setVisible(true);
    manageModsButton.setOnAction(event -> manageMods());
  }

  @Override
  protected void onDisplayDetails(ModVersion modVersion) {
    JavaFxUtil.assertApplicationThread();
    modDetailController.setModVersion(modVersion);
    modDetailController.getRoot().setVisible(true);
    modDetailController.getRoot().requestFocus();
  }

  protected void setSupplier(SearchConfig searchConfig) {
    switch (searchType) {
      case SEARCH:
        currentSupplier = modService.findByQueryWithPageCount(searchConfig, pageSize, pagination.getCurrentPageIndex() + 1);
        break;
      case NEWEST:
        currentSupplier = modService.getNewestModsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
        break;
      case HIGHEST_RATED:
        currentSupplier = modService.getHighestRatedModsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
        break;
      case HIGHEST_RATED_UI:
        currentSupplier = modService.getHighestRatedUiModsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
        break;
    }
  }

  protected Node getEntityCard(ModVersion modVersion) {
    ModCardController controller = uiService.loadFxml("theme/vault/mod/mod_card.fxml");
    controller.setModVersion(modVersion);
    controller.setOnOpenDetailListener(this::onDisplayDetails);
    return controller.getRoot();
  }

  @Override
  protected List<ShowRoomCategory> getShowRoomCategories() {
    return Arrays.asList(
        new ShowRoomCategory(() -> modService.getHighestRatedModsWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.HIGHEST_RATED, "modVault.highestRated"),
        new ShowRoomCategory(() -> modService.getHighestRatedUiModsWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.HIGHEST_RATED_UI, "modVault.highestRatedUiMods"),
        new ShowRoomCategory(() -> modService.getNewestModsWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.NEWEST, "modVault.newestMods")
    );
  }

  public void onUploadButtonClicked() {
    Platform.runLater(() -> {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setInitialDirectory(preferencesService.getPreferences().getForgedAlliance().getModsDirectory().toFile());
      directoryChooser.setTitle(i18n.get("modVault.upload.chooseDirectory"));
      File result = directoryChooser.showDialog(getRoot().getScene().getWindow());

      if (result == null) {
        return;
      }
      openUploadWindow(result.toPath());
    });
  }

  @Override
  protected Node getDetailView() {
    modDetailController = uiService.loadFxml("theme/vault/mod/mod_detail.fxml");
    return modDetailController.getRoot();
  }

  protected void initSearchController() {
    searchController.setRootType(com.faforever.client.api.dto.Mod.class);
    searchController.setSearchableProperties(SearchablePropertyMappings.MOD_PROPERTY_MAPPING);
    searchController.setSortConfig(preferencesService.getPreferences().getVaultPrefs().mapSortConfigProperty());
    searchController.setOnlyShowLastYearCheckBoxVisible(false);
    searchController.setVaultRoot(vaultRoot);
    searchController.setSavedQueries(preferencesService.getPreferences().getVaultPrefs().getSavedModQueries());

    searchController.addTextFilter("displayName", i18n.get("mod.displayName"));
    searchController.addTextFilter("author", i18n.get("mod.author"));
    searchController.addDateRangeFilter("latestVersion.updateTime", i18n.get("mod.uploadedDateTime"), 0);

    searchController.addBinaryFilter("latestVersion.width", i18n.get("map.width"),
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
  }

  public void manageMods() {
    ModManagerController modManagerController = uiService.loadFxml("theme/mod_manager.fxml");
    Dialog dialog = uiService.showInDialog(vaultRoot, modManagerController.getRoot(), i18n.get("modVault.modManager"));
    dialog.setOnDialogClosed(event -> modManagerController.apply());
  }

  @Subscribe
  public void onModUploaded(ModUploadedEvent event) {
    onRefreshButtonClicked();
  }
}
