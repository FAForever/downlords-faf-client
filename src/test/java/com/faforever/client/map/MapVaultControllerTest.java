package com.faforever.client.map;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Tuple;
import com.faforever.client.vault.VaultEntityController.SearchType;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapVaultControllerTest extends AbstractPlainJavaFxTest {

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
  private PlayerService playerService;
  @Mock
  private ReportingService reportingService;

  private MapVaultController instance;
  private SortConfig sortOrder;
  private SearchConfig standardSearchConfig;
  private MapDetailController mapDetailController;

  @Before
  public void setUp() throws Exception {
    when(preferencesService.getPreferences()).thenReturn(new Preferences());

    doAnswer(invocation -> {
      mapDetailController = mock(MapDetailController.class);
      when(mapDetailController.getRoot()).then(invocation1 -> new Pane());
      return mapDetailController;
    }).when(uiService).loadFxml("theme/vault/map/map_detail.fxml");

    instance = new MapVaultController(mapService, i18n, eventBus, preferencesService, uiService, notificationService,reportingService);
    sortOrder = preferencesService.getPreferences().getVaultPrefs().getMapSortConfig();
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
    when(mapService.getLadderMapsWithPageCount(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), 0)));
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
    instance.searchType = SearchType.LADDER;
    instance.setSupplier(null);

    verify(mapService).findByQueryWithPageCount(standardSearchConfig, instance.pageSize, 1);
    verify(mapService).getHighestRatedMapsWithPageCount(instance.pageSize, 1);
    verify(mapService).getRecommendedMapsWithPageCount(instance.pageSize, 1);
    verify(mapService).getNewestMapsWithPageCount(instance.pageSize, 1);
    verify(mapService).getMostPlayedMapsWithPageCount(instance.pageSize, 1);
    verify(mapService).getOwnedMapsWithPageCount(instance.pageSize, 1);
    verify(mapService).getLadderMapsWithPageCount(instance.pageSize, 1);
  }

  @Test
  public void testShowMapDetail() {
    MapBean mapBean = MapBeanBuilder.create().defaultValues().get();
    Platform.runLater(()-> instance.onDisplayDetails(mapBean));
    WaitForAsyncUtils.waitForFxEvents();

    verify(mapDetailController).setMap(mapBean);
    assertThat(mapDetailController.getRoot().isVisible(), is(true));
  }
}
