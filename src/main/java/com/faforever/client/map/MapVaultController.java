package com.faforever.client.map;

import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.server.MatchmakerQueueInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenMapVaultEvent;
import com.faforever.client.main.event.ShowMapPoolEvent;
import com.faforever.client.map.management.MapsManagementController;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MapVaultController extends VaultEntityController<MapVersion> {

  private final MapService mapService;
  private final PlatformService platformService;
  private final VaultPrefs vaultPrefs;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  private MapDetailController mapDetailController;
  private Integer recommendedShowRoomPageCount;
  private MatchmakerQueueInfo matchmakerQueue;

  public MapVaultController(MapService mapService, I18n i18n,
                            UiService uiService, NotificationService notificationService,
                            ReportingService reportingService,
                            PlatformService platformService, VaultPrefs vaultPrefs,
                            ForgedAlliancePrefs forgedAlliancePrefs,
                            FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    super(uiService, notificationService, i18n, reportingService, vaultPrefs, fxApplicationThreadExecutor);
    this.mapService = mapService;
    this.platformService = platformService;
    this.vaultPrefs = vaultPrefs;
    this.forgedAlliancePrefs = forgedAlliancePrefs;
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    manageVaultButton.setVisible(true);
    manageVaultButton.setText(i18n.get("management.maps.openButton.label"));
    mapService.getRecommendedMapPageCount(TOP_ELEMENT_COUNT)
              .subscribe(numPages -> recommendedShowRoomPageCount = numPages);
  }

  @Override
  protected void initSearchController() {
    searchController.setRootType(com.faforever.commons.api.dto.Map.class);
    searchController.setSearchableProperties(SearchablePropertyMappings.MAP_PROPERTY_MAPPING);
    searchController.setSortConfig(vaultPrefs.mapSortConfigProperty());
    searchController.setOnlyShowLastYearCheckBoxVisible(false);
    searchController.setVaultRoot(vaultRoot);
    searchController.setSavedQueries(vaultPrefs.getSavedMapQueries());

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
    searchController.addRangeFilter("latestVersion.maxPlayers", i18n.get("map.maxPlayers"), 0, 16, 16, 0, 0, Double::intValue);
    searchController.addRangeFilter("reviewsSummary.averageScore", i18n.get("reviews.averageScore"), 0, 5, 10, 4, 1);
    searchController.addToggleFilter("latestVersion.ranked", i18n.get("map.onlyRanked"), "true");
  }

  @Override
  protected void onDisplayDetails(MapVersion mapBean) {
    JavaFxUtil.assertApplicationThread();
    mapDetailController.setMapVersion(mapBean);
    mapDetailController.getRoot().setVisible(true);
    mapDetailController.getRoot().requestFocus();
  }

  @Override
  protected void setSupplier(SearchConfig searchConfig) {
    currentSupplier = switch (searchType) {
      case SEARCH -> mapService.findByQueryWithPageCount(searchConfig, pageSize, pagination.getCurrentPageIndex() + 1);
      case RECOMMENDED -> mapService.getRecommendedMapsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case NEWEST -> mapService.getNewestMapsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case HIGHEST_RATED -> mapService.getHighestRatedMapsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case PLAYED -> mapService.getMostPlayedMapsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case OWN -> mapService.getOwnedMapsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case PLAYER, HIGHEST_RATED_UI -> throw new UnsupportedOperationException();
    };
  }

  @Override
  protected VaultEntityCardController<MapVersion> createEntityCard() {
    MapCardController controller = uiService.loadFxml("theme/vault/map/map_card.fxml");
    controller.setOnOpenDetailListener(this::onDisplayDetails);
    return controller;
  }

  @Override
  protected List<ShowRoomCategory<MapVersion>> getShowRoomCategories() {
return List.of(
    new ShowRoomCategory<>(() -> {
      int recommendedPage;
      if (recommendedShowRoomPageCount != null && recommendedShowRoomPageCount > 0) {
        recommendedPage = new Random().nextInt(recommendedShowRoomPageCount) + 1;
      } else {
        recommendedPage = 1;
      }
      return mapService.getRecommendedMapsWithPageCount(TOP_ELEMENT_COUNT, recommendedPage);
    }, SearchType.RECOMMENDED, "mapVault.recommended"),
    new ShowRoomCategory<>(() -> mapService.getHighestRatedMapsWithPageCount(TOP_ELEMENT_COUNT, 1),
                           SearchType.HIGHEST_RATED, "mapVault.mostLikedMaps"),
    new ShowRoomCategory<>(() -> mapService.getNewestMapsWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.NEWEST,
                           "mapVault.newestMaps"),
    new ShowRoomCategory<>(() -> mapService.getMostPlayedMapsWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.PLAYED,
                           "mapVault.mostPlayed"),
    new ShowRoomCategory<>(() -> mapService.getOwnedMapsWithPageCount(TOP_ELEMENT_COUNT, 1), SearchType.OWN,
                           "mapVault.owned"));
  }

  @Override
  public void onUploadButtonClicked() {
    platformService.askForPath(i18n.get("mapVault.upload.chooseDirectory"), forgedAlliancePrefs.getMapsDirectory())
                   .thenAccept(possiblePath -> possiblePath.ifPresent(this::openUploadWindow));
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
    log.warn("No such NavigateEvent for this Controller: {}", navigateEvent.getClass());
  }

  private void openUploadWindow(Path path) {
    MapUploadController mapUploadController = uiService.loadFxml("theme/vault/map/map_upload.fxml");
    mapUploadController.setMapPath(path);

    Node root = mapUploadController.getRoot();
    Dialog dialog = uiService.showInDialog(vaultRoot, root, i18n.get("mapVault.upload.title"));
    uiService.makeScrollableDialog(dialog);
    mapUploadController.setOnCancelButtonClickedListener(dialog::close);
    mapUploadController.setUploadListener(this::onRefreshButtonClicked);
  }
}
