package com.faforever.client.map;

import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.VaultPrefs;
import com.faforever.client.query.CategoryFilterController;
import com.faforever.client.query.DateRangeFilterController;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.RangeFilterController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.query.TextFilterController;
import com.faforever.client.query.ToggleFilterController;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.VaultEntityController;
import com.faforever.client.vault.VaultEntityController.SearchType;
import com.faforever.client.vault.VaultEntityController.ShowRoomCategory;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.Pane;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
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
  @Mock
  private CategoryFilterController categoryFilterController;
  @Mock
  private DateRangeFilterController dateRangeFilterController;
  @Mock
  private TextFilterController textFilterController;
  @Mock
  private RangeFilterController rangeFilterController;
  @Mock
  private ToggleFilterController toggleFilterController;
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
    when(searchController.addCategoryFilter(any(), any(), anyMap())).thenReturn(categoryFilterController);
    when(categoryFilterController.getCheckedItems()).thenReturn(new SimpleListProperty<String>());
    when(searchController.addTextFilter(anyString(), anyString(), anyBoolean())).thenReturn(textFilterController);
    when(textFilterController.textFieldProperty()).thenReturn(new SimpleStringProperty());
    when(searchController.addRangeFilter(anyString(), anyString(), anyDouble(), anyDouble(), anyInt(), anyInt(), anyInt())).thenReturn(rangeFilterController);
    when(searchController.addRangeFilter(anyString(), anyString(), anyDouble(), anyDouble(), anyInt(), anyInt(), anyInt(), any())).thenReturn(rangeFilterController);
    when(rangeFilterController.lowValueProperty()).thenReturn(new SimpleDoubleProperty());
    when(rangeFilterController.highValueProperty()).thenReturn(new SimpleDoubleProperty());
    when(searchController.addToggleFilter(anyString(), anyString(), anyString())).thenReturn(toggleFilterController);
    when(toggleFilterController.selectedProperty()).thenReturn(new SimpleBooleanProperty());
    when(searchController.addDateRangeFilter(anyString(), anyString(), anyInt())).thenReturn(dateRangeFilterController);
    when(dateRangeFilterController.beforeDateProperty()).thenReturn(new SimpleObjectProperty<LocalDate>());
    when(dateRangeFilterController.afterDateProperty()).thenReturn(new SimpleObjectProperty<LocalDate>());
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
    lenient().when(mapService.getHighestRatedMapsWithPageCount(anyInt(), anyInt())).thenReturn(
        Mono.zip(Mono.just(List.<MapVersion>of()), Mono.just(0)));
    lenient().when(mapService.getNewestMapsWithPageCount(anyInt(), anyInt())).thenReturn(
        Mono.zip(Mono.just(List.<MapVersion>of()), Mono.just(0)));
    lenient().when(mapService.getMostPlayedMapsWithPageCount(anyInt(), anyInt())).thenReturn(
        Mono.zip(Mono.just(List.<MapVersion>of()), Mono.just(0)));
    lenient().when(mapService.getRecommendedMapsWithPageCount(anyInt(), anyInt())).thenReturn(
        Mono.zip(Mono.just(List.<MapVersion>of()), Mono.just(0)));
    lenient().when(mapService.getOwnedMapsWithPageCount(anyInt(), anyInt())).thenReturn(
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
    MapVersion mapBean = Instancio.create(MapVersion.class);
    runOnFxThreadAndWait(() -> instance.onDisplayDetails(mapBean));

    verify(mapDetailController).setMapVersion(mapBean);
    assertTrue(mapDetailController.getRoot().isVisible());
  }

  @Test
  public void testGetShowRoomCategories() {
    List<VaultEntityController.ShowRoomCategory<MapVersion>> categories = instance.getShowRoomCategories();
    for (ShowRoomCategory<MapVersion> category : categories) {
      category.entitySupplier().get();
    }
    verify(mapService).getHighestRatedMapsWithPageCount(anyInt(), anyInt());
    verify(mapService).getNewestMapsWithPageCount(anyInt(), anyInt());
    verify(mapService).getMostPlayedMapsWithPageCount(anyInt(), anyInt());
    verify(mapService).getRecommendedMapsWithPageCount(anyInt(), anyInt());
    verify(mapService).getOwnedMapsWithPageCount(anyInt(), anyInt());
  }
}
