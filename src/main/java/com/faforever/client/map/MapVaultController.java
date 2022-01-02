package com.faforever.client.map;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenMapVaultEvent;
import com.faforever.client.main.event.ShowMapPoolEvent;
import com.faforever.client.map.event.MapUploadedEvent;
import com.faforever.client.map.management.MapsManagementController;
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
import javafx.scene.Node;
import javafx.stage.DirectoryChooser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
// TODO I'd like to avoid the additional "getMost*" methods and always use the map query function instead, however,
// this is currently not viable since Elide can't yet sort by relationship attributes. Once it supports that
// (see https://github.com/yahoo/elide/issues/353), this can be refactored.
public class MapVaultController extends VaultEntityController<MapVersionBean> {

  private final MapService mapService;
  private final EventBus eventBus;

  private MapDetailController mapDetailController;
  private Integer recommendedShowRoomPageCount;
  private MatchmakerQueueBean matchmakerQueue;

  public MapVaultController(MapService mapService, I18n i18n, EventBus eventBus, PreferencesService preferencesService,
                            UiService uiService, NotificationService notificationService, ReportingService reportingService) {
    super(uiService, notificationService, i18n, preferencesService, reportingService);
    this.mapService = mapService;
    this.eventBus = eventBus;
  }

  @Override
  public void initialize() {
    super.initialize();
    manageVaultButton.setVisible(true);
    manageVaultButton.setText(i18n.get("management.maps.openButton.label"));
    mapService.getRecommendedMapPageCount(TOP_ELEMENT_COUNT).thenAccept(numPages -> recommendedShowRoomPageCount = numPages);

    eventBus.register(this);
  }

  protected void initSearchController() {
    searchController.setRootType(com.faforever.commons.api.dto.Map.class);
    searchController.setSearchableProperties(SearchablePropertyMappings.MAP_PROPERTY_MAPPING);
    searchController.setSortConfig(preferencesService.getPreferences().getVault().mapSortConfigProperty());
    searchController.setOnlyShowLastYearCheckBoxVisible(false);
    searchController.setVaultRoot(vaultRoot);
    searchController.setSavedQueries(preferencesService.getPreferences().getVault().getSavedMapQueries());

    searchController.addTextFilter("displayName", i18n.get("map.name"), false);
    searchController.addTextFilter("author.login", i18n.get("map.author"), false);
    searchController.addDateRangeFilter("latestVersion.updateTime", i18n.get("map.uploadedDateTime"), 0);

    LinkedHashMap<String, String> mapSizeMap = new LinkedHashMap<>();
    mapSizeMap.put("1km", "64");
    mapSizeMap.put("2km", "128");
    mapSizeMap.put("5km", "256");
    mapSizeMap.put("10km", "512");
    mapSizeMap.put("20km", "1024");
    mapSizeMap.put("40km", "2048");
    mapSizeMap.put("80km", "4096");

    searchController.addCategoryFilter("latestVersion.width", i18n.get("map.width"), mapSizeMap);
    searchController.addCategoryFilter("latestVersion.height", i18n.get("map.height"), mapSizeMap);
    searchController.addRangeFilter("latestVersion.maxPlayers", i18n.get("map.maxPlayers"), 0, 16, 1, Double::intValue);
    searchController.addToggleFilter("latestVersion.ranked", i18n.get("map.onlyRanked"), "true");
  }

  @Override
  protected void onDisplayDetails(MapVersionBean mapBean) {
    JavaFxUtil.assertApplicationThread();
    mapDetailController.setMapVersion(mapBean);
    mapDetailController.getRoot().setVisible(true);
    mapDetailController.getRoot().requestFocus();
  }

  protected void setSupplier(SearchConfig searchConfig) {
    switch (searchType) {
      case SEARCH -> currentSupplier = mapService.findByQueryWithPageCount(searchConfig, pageSize, pagination.getCurrentPageIndex() + 1);
      case RECOMMENDED -> currentSupplier = mapService.getRecommendedMapsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case NEWEST -> currentSupplier = mapService.getNewestMapsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case HIGHEST_RATED -> currentSupplier = mapService.getHighestRatedMapsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case PLAYED -> currentSupplier = mapService.getMostPlayedMapsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case MAP_POOL -> currentSupplier = mapService.getMatchmakerMapsWithPageCount(matchmakerQueue, pageSize, pagination.getCurrentPageIndex() + 1);
      case OWN -> currentSupplier = mapService.getOwnedMapsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
    }
  }

  protected Node getEntityCard(MapVersionBean map) {
    MapCardController controller = uiService.loadFxml("theme/vault/map/map_card.fxml");
    controller.setMapVersion(map);
    controller.setOnOpenDetailListener(this::onDisplayDetails);
    return controller.getRoot();
  }

  @Override
  protected List<ShowRoomCategory> getShowRoomCategories() {
    int recommendedPage;
    if (recommendedShowRoomPageCount != null && recommendedShowRoomPageCount > 0) {
      recommendedPage = new Random().nextInt(recommendedShowRoomPageCount) + 1;
    } else {
      recommendedPage = 1;
    }

    return Arrays.asList(
        new ShowRoomCategory(() -> mapService.getRecommendedMapsWithPageCount(TOP_ELEMENT_COUNT, recommendedPage), SearchType.RECOMMENDED, "mapVault.recommended"),
        new ShowRoomCategory(() -> mapService.getHighestRatedMapsWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.HIGHEST_RATED, "mapVault.mostLikedMaps"),
        new ShowRoomCategory(() -> mapService.getNewestMapsWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.NEWEST, "mapVault.newestMaps"),
        new ShowRoomCategory(() -> mapService.getMostPlayedMapsWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.PLAYED, "mapVault.mostPlayed"),
        new ShowRoomCategory(() -> mapService.getOwnedMapsWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.OWN, "mapVault.owned"));
  }

  public void onUploadButtonClicked() {
    JavaFxUtil.runLater(() -> {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setInitialDirectory(preferencesService.getPreferences().getForgedAlliance().getMapsDirectory().toFile());
      directoryChooser.setTitle(i18n.get("mapVault.upload.chooseDirectory"));
      File result = directoryChooser.showDialog(getRoot().getScene().getWindow());

      if (result == null) {
        return;
      }
      openUploadWindow(result.toPath());
    });
  }

  @Override
  protected void onManageVaultButtonClicked() {
    MapsManagementController controller = uiService.loadFxml("theme/vault/map/maps_management.fxml");
    Dialog dialog = uiService.showInDialog(vaultRoot, controller.getRoot());
    controller.setCloseButtonAction(dialog::close);
  }

  @Override
  protected Node getDetailView() {
    mapDetailController = uiService.loadFxml("theme/vault/map/map_detail.fxml");
    return mapDetailController.getRoot();
  }

  @Override
  protected Class<? extends NavigateEvent> getDefaultNavigateEvent() {
    return OpenMapVaultEvent.class;
  }

  @Override
  protected void handleSpecialNavigateEvent(NavigateEvent navigateEvent) {
    if (navigateEvent instanceof ShowMapPoolEvent) {
      matchmakerQueue = ((ShowMapPoolEvent) navigateEvent).getQueue();
      searchType = SearchType.MAP_POOL;
      onPageChange(null, true);
    } else {
      log.warn("No such NavigateEvent for this Controller: {}", navigateEvent.getClass());
    }
  }

  private void openUploadWindow(Path path) {
    MapUploadController mapUploadController = uiService.loadFxml("theme/vault/map/map_upload.fxml");
    mapUploadController.setMapPath(path);

    Node root = mapUploadController.getRoot();
    Dialog dialog = uiService.showInDialog(vaultRoot, root, i18n.get("mapVault.upload.title"));
    uiService.makeScrollableDialog(dialog);
    mapUploadController.setOnCancelButtonClickedListener(dialog::close);
  }

  @Subscribe
  public void onMapUploaded(MapUploadedEvent event) {
    onRefreshButtonClicked();
  }
}
