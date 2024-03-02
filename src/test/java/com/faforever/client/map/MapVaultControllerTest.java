package com.faforever.client.map;

import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.VaultPrefs;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.VaultEntityController;
import com.faforever.client.vault.VaultEntityController.SearchType;
import com.faforever.client.vault.VaultEntityController.ShowRoomCategory;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import javafx.scene.layout.Pane;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapVaultControllerTest extends PlatformTest {

  @Mock
  private MapService mapService;
  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;

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
  @Mock
  private PlatformService platformService;
  @Spy
  private VaultPrefs vaultPrefs;

  @InjectMocks
  private MapVaultController instance;
  private SearchConfig standardSearchConfig;
  private MapDetailController mapDetailController;

  @BeforeEach
  public void setUp() throws Exception {
    when(i18n.get(anyString())).thenReturn("test");

    doAnswer(invocation -> {
      mapDetailController = mock(MapDetailController.class);
      when(mapDetailController.getRoot()).then(invocation1 -> new Pane());
      return mapDetailController;
    }).when(uiService).loadFxml("theme/vault/map/map_detail.fxml");

    when(mapService.getRecommendedMapPageCount(VaultEntityController.TOP_ELEMENT_COUNT)).thenReturn(Mono.just(0));

    SortConfig sortOrder = new Preferences().getVault().getMapSortConfig();
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
    when(mapService.getHighestRatedMapsWithPageCount(anyInt(), anyInt())).thenReturn(
        Mono.zip(Mono.just(List.<MapVersionBean>of()), Mono.just(0)));
    when(mapService.getNewestMapsWithPageCount(anyInt(), anyInt())).thenReturn(
        Mono.zip(Mono.just(List.<MapVersionBean>of()), Mono.just(0)));
    when(mapService.getMostPlayedMapsWithPageCount(anyInt(), anyInt())).thenReturn(
        Mono.zip(Mono.just(List.<MapVersionBean>of()), Mono.just(0)));
    when(mapService.getRecommendedMapsWithPageCount(anyInt(), anyInt())).thenReturn(
        Mono.zip(Mono.just(List.<MapVersionBean>of()), Mono.just(0)));
    when(mapService.getOwnedMapsWithPageCount(anyInt(), anyInt())).thenReturn(
        Mono.zip(Mono.just(List.of()), Mono.just(0)));
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
    MapVersionBean mapBean = Instancio.create(MapVersionBean.class);
    runOnFxThreadAndWait(() -> instance.onDisplayDetails(mapBean));

    verify(mapDetailController).setMapVersion(mapBean);
    assertTrue(mapDetailController.getRoot().isVisible());
  }

  @Test
  public void testGetShowRoomCategories() {
    List<VaultEntityController.ShowRoomCategory<MapVersionBean>> categories = instance.getShowRoomCategories();
    for (ShowRoomCategory<MapVersionBean> category : categories) {
      category.entitySupplier().get();
    }
    verify(mapService).getHighestRatedMapsWithPageCount(anyInt(), anyInt());
    verify(mapService).getNewestMapsWithPageCount(anyInt(), anyInt());
    verify(mapService).getMostPlayedMapsWithPageCount(anyInt(), anyInt());
    verify(mapService).getRecommendedMapsWithPageCount(anyInt(), anyInt());
    verify(mapService).getOwnedMapsWithPageCount(anyInt(), anyInt());
  }
}
