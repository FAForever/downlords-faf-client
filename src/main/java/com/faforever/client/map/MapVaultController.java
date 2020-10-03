package com.faforever.client.map;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.OpenMapVaultEvent;
import com.faforever.client.main.event.ShowLadderMapsEvent;
import com.faforever.client.map.event.MapUploadedEvent;
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
import java.util.Random;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
// TODO I'd like to avoid the additional "getMost*" methods and always use the map query function instead, however,
// this is currently not viable since Elide can't yet sort by relationship attributes. Once it supports that
// (see https://github.com/yahoo/elide/issues/353), this can be refactored.
public class MapVaultController extends VaultEntityController<MapBean> {

  private final MapService mapService;
  private final EventBus eventBus;

  private MapDetailController mapDetailController;
  private Integer recommendedShowRoomPageCount;

  public MapVaultController(MapService mapService, I18n i18n, EventBus eventBus, PreferencesService preferencesService,
                            UiService uiService, NotificationService notificationService, ReportingService reportingService) {
    super(uiService, notificationService, i18n, preferencesService, reportingService);
    this.mapService = mapService;
    this.eventBus = eventBus;
  }

  @Override
  public void initialize() {
    super.initialize();

    searchController.setRootType(com.faforever.client.api.dto.Map.class);
    searchController.setSearchableProperties(SearchablePropertyMappings.MAP_PROPERTY_MAPPING);
    searchController.setSortConfig(preferencesService.getPreferences().getVaultPrefs().mapSortConfigProperty());
    searchController.setOnlyShowLastYearCheckBoxVisible(false, false);

    preferencesService.getRemotePreferencesAsync().thenAccept(clientConfiguration ->
        recommendedShowRoomPageCount = clientConfiguration.getRecommendedMaps().size() / TOP_ELEMENT_COUNT)
        .exceptionally(throwable -> {
          recommendedShowRoomPageCount = null;
          log.warn("Client Configuration get recommended maps failed", throwable);
          return null;
        });

    eventBus.register(this);
  }

  @Override
  protected void onDisplayDetails(MapBean mapBean) {
    JavaFxUtil.assertApplicationThread();
    mapDetailController.setMap(mapBean);
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
      case LADDER -> currentSupplier = mapService.getLadderMapsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
      case OWN -> currentSupplier = mapService.getOwnedMapsWithPageCount(pageSize, pagination.getCurrentPageIndex() + 1);
    }
  }

  protected Node getEntityCard(MapBean map) {
    MapCardController controller = uiService.loadFxml("theme/vault/map/map_card.fxml");
    controller.setMap(map);
    controller.setOnOpenDetailListener(this::onDisplayDetails);
    return controller.getRoot();
  }

  @Override
  protected List<ShowRoomCategory> getShowRoomCategories() {
    Random random = new Random();
    int recommendedPage;
    if (recommendedShowRoomPageCount != null && recommendedShowRoomPageCount > 0) {
      recommendedPage = random.nextInt(recommendedShowRoomPageCount) + 1;
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
    Platform.runLater(() -> {
      DirectoryChooser directoryChooser = new DirectoryChooser();
      directoryChooser.setInitialDirectory(preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory().toFile());
      directoryChooser.setTitle(i18n.get("mapVault.upload.chooseDirectory"));
      File result = directoryChooser.showDialog(getRoot().getScene().getWindow());

      if (result == null) {
        return;
      }
      openUploadWindow(result.toPath());
    });
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
    if (navigateEvent instanceof ShowLadderMapsEvent) {
      searchType = SearchType.LADDER;
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
    mapUploadController.setOnCancelButtonClickedListener(dialog::close);
  }

  @Subscribe
  public void onMapUploaded(MapUploadedEvent event) {
    onRefreshButtonClicked();
  }
}