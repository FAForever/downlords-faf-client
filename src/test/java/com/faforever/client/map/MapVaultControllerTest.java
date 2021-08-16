package com.faforever.client.map;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.VaultEntityController;
import com.faforever.client.vault.VaultEntityController.SearchType;
import com.faforever.client.vault.VaultEntityController.ShowRoomCategory;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.google.common.eventbus.EventBus;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapVaultControllerTest extends UITest {

  @Mock
  private MapService mapService;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private SearchController searchController;
  @Mock
  private LogicalNodeController logicalNodeController;
  @Mock
  private SpecificationController specificationController;
  @Mock
  private ReportingService reportingService;

  private MapVaultController instance;
  private SearchConfig standardSearchConfig;
  private MapDetailController mapDetailController;

  @BeforeEach
  public void setUp() throws Exception {
    when(preferencesService.getPreferences()).thenReturn(new Preferences());
    when(i18n.get(anyString())).thenReturn("test");

    doAnswer(invocation -> {
      mapDetailController = mock(MapDetailController.class);
      when(mapDetailController.getRoot()).then(invocation1 -> new Pane());
      return mapDetailController;
    }).when(uiService).loadFxml("theme/vault/map/map_detail.fxml");

    when(mapService.getRecommendedMapPageCount(VaultEntityController.TOP_ELEMENT_COUNT)).thenReturn(CompletableFuture.completedFuture(0));

    instance = new MapVaultController(mapService, i18n, eventBus, preferencesService, uiService, notificationService, reportingService);
    SortConfig sortOrder = preferencesService.getPreferences().getVault().getMapSortConfig();
    standardSearchConfig = new SearchConfig(sortOrder, "query");


    loadFxml("theme/vault/vault_entity.fxml", clazz -> {
      if (clazz == SearchController.class) {
        return searchController;
      }
      if (clazz == LogicalNodeController.class) {
        return logicalNodeController;
      }
      if (clazz == SpecificationController.class) {
        return specificationController;
      }
      return instance;
    }, instance);
    when(mapService.getHighestRatedMapsWithPageCount(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), 0)));
    when(mapService.getNewestMapsWithPageCount(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), 0)));
    when(mapService.getMostPlayedMapsWithPageCount(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), 0)));
    when(mapService.getRecommendedMapsWithPageCount(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), 0)));
    when(mapService.getOwnedMapsWithPageCount(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), 0)));
  }

  @Test
  public void testSetSupplier() {
    instance.searchType = SearchType.SEARCH;
    instance.setSupplier(standardSearchConfig);
    instance.searchType = SearchType.NEWEST;
    instance.setSupplier(null);
    instance.searchType = SearchType.HIGHEST_RATED;
    instance.setSupplier(null);
    instance.searchType = SearchType.PLAYED;
    instance.setSupplier(null);
    instance.searchType = SearchType.RECOMMENDED;
    instance.setSupplier(null);
    instance.searchType = SearchType.OWN;
    instance.setSupplier(null);

    verify(mapService).findByQueryWithPageCount(standardSearchConfig, instance.pageSize, 1);
    verify(mapService).getHighestRatedMapsWithPageCount(instance.pageSize, 1);
    verify(mapService).getRecommendedMapsWithPageCount(instance.pageSize, 1);
    verify(mapService).getNewestMapsWithPageCount(instance.pageSize, 1);
    verify(mapService).getMostPlayedMapsWithPageCount(instance.pageSize, 1);
    verify(mapService).getOwnedMapsWithPageCount(instance.pageSize, 1);
  }

  @Test
  public void testShowMapDetail() throws MalformedURLException {
    MapBean mapBean = MapBeanBuilder.create().defaultValues().get();
    JavaFxUtil.runLater(() -> instance.onDisplayDetails(mapBean));
    WaitForAsyncUtils.waitForFxEvents();

    verify(mapDetailController).setMap(mapBean);
    assertTrue(mapDetailController.getRoot().isVisible());
  }

  @Test
  public void testGetShowRoomCategories() {
    List<VaultEntityController<MapBean>.ShowRoomCategory> categories = instance.getShowRoomCategories();
    for (ShowRoomCategory category : categories) {
      category.getEntitySupplier().get();
    }
    verify(mapService).getHighestRatedMapsWithPageCount(anyInt(), anyInt());
    verify(mapService).getNewestMapsWithPageCount(anyInt(), anyInt());
    verify(mapService).getMostPlayedMapsWithPageCount(anyInt(), anyInt());
    verify(mapService).getRecommendedMapsWithPageCount(anyInt(), anyInt());
    verify(mapService).getOwnedMapsWithPageCount(anyInt(), anyInt());
  }
}
