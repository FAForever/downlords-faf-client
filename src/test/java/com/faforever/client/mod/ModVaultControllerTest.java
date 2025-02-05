package com.faforever.client.mod;

import com.faforever.client.domain.api.ModVersion;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.VaultPrefs;
import com.faforever.client.query.BinaryFilterController;
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
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.Pane;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModVaultControllerTest extends PlatformTest {

  @InjectMocks
  private ModVaultController instance;

  @Mock
  private ModService modService;
  @Mock
  private UiService uiService;
  @Mock
  private I18n i18n;

  @Mock
  private NotificationService notificationService;
  @Mock
  private LogicalNodeController logicalNodeController;
  @Mock
  private SearchController searchController;
  @Mock
  private SpecificationController specificationController;
  @Mock
  private BinaryFilterController binaryFilterController;
  @Mock
  private DateRangeFilterController dateRangeFilterController;
  @Mock
  private RangeFilterController rangeFilterController;
  @Mock
  private TextFilterController textFilterController;
  @Mock
  private ToggleFilterController toggleFilterController;
  @Mock
  private ReportingService reportingService;
  @Mock
  private PlatformService platformService;
  @Spy
  private VaultPrefs vaultPrefs;

  @Mock
  private ModDetailController modDetailController;

  @BeforeEach
  public void setUp() throws Exception {
    when(modService.getNewestModsWithPageCount(anyInt(), anyInt())).thenReturn(
        Mono.zip(Mono.just(List.of()), Mono.just(0)));
    when(modService.getHighestRatedModsWithPageCount(anyInt(), anyInt())).thenReturn(
        Mono.zip(Mono.just(List.of()), Mono.just(0)));
    when(modService.getHighestRatedUiModsWithPageCount(anyInt(), anyInt())).thenReturn(
        Mono.zip(Mono.just(List.of()), Mono.just(0)));
    when(i18n.get(anyString())).thenReturn("test");
    when(modDetailController.getRoot()).thenReturn(new Pane());
    when(searchController.addBinaryFilter(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(binaryFilterController);
    when(binaryFilterController.firstSelectedProperty()).thenReturn(new SimpleBooleanProperty());
    when(binaryFilterController.secondSelectedProperty()).thenReturn(new SimpleBooleanProperty());
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

    when(uiService.loadFxml("theme/vault/mod/mod_detail.fxml")).thenReturn(modDetailController);

    when(modService.getRecommendedModPageCount(VaultEntityController.TOP_ELEMENT_COUNT)).thenReturn(Mono.just(0));

    loadFxml("theme/vault/vault_entity.fxml", clazz -> {
      if (SearchController.class.isAssignableFrom(clazz)) {
        return searchController;
      }
      if (SpecificationController.class.isAssignableFrom(clazz)) {
        return specificationController;
      }
      if (LogicalNodeController.class.isAssignableFrom(clazz)) {
        return logicalNodeController;
      }
      return instance;
    }, instance);
  }

  @Test
  public void testSetSupplier() {
    SortConfig sortOrder = vaultPrefs.getMapSortConfig();
    SearchConfig standardSearchConfig = new SearchConfig(sortOrder, "query");
    instance.searchType = SearchType.SEARCH;
    instance.setSupplier(standardSearchConfig);
    instance.searchType = SearchType.NEWEST;
    instance.setSupplier(null);
    instance.searchType = SearchType.HIGHEST_RATED;
    instance.setSupplier(null);
    instance.searchType = SearchType.HIGHEST_RATED_UI;
    instance.setSupplier(null);

    verify(modService).findByQueryWithPageCount(standardSearchConfig, instance.pageSize, 1);
    verify(modService).getHighestRatedModsWithPageCount(instance.pageSize, 1);
    verify(modService).getHighestRatedUiModsWithPageCount(instance.pageSize, 1);
    verify(modService).getNewestModsWithPageCount(instance.pageSize, 1);
  }

  @Test
  @Disabled("Flaky Test")
  public void testShowModDetail() throws MalformedURLException {
    ModVersion modVersion = Instancio.create(ModVersion.class);
    runOnFxThreadAndWait(() -> instance.onDisplayDetails(modVersion));

    verify(modDetailController).setModVersion(modVersion);
    assertThat(modDetailController.getRoot().isVisible(), is(true));
  }
}
