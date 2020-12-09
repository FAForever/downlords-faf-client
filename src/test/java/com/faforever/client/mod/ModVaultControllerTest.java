package com.faforever.client.mod;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModVaultControllerTest extends AbstractPlainJavaFxTest {

  private ModVaultController instance;

  @Mock
  private ModService modService;
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
  private LogicalNodeController logicalNodeController;
  @Mock
  private SearchController searchController;
  @Mock
  private SpecificationController specificationController;
  @Mock
  private ReportingService reportingService;

  private SortConfig sortOrder;
  private SearchConfig standardSearchConfig;
  private ModDetailController modDetailController;

  @Before
  public void setUp() throws Exception {
    when(preferencesService.getPreferences()).thenReturn(new Preferences());
    when(modService.getNewestModsWithPageCount(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), 0)));
    when(modService.getHighestRatedModsWithPageCount(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), 0)));
    when(modService.getHighestRatedUiModsWithPageCount(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), 0)));
    when(i18n.get(anyString())).thenReturn("test");

    sortOrder = preferencesService.getPreferences().getVaultPrefs().getMapSortConfig();
    standardSearchConfig = new SearchConfig(sortOrder, "query");
    instance = new ModVaultController(modService, i18n, eventBus, preferencesService, uiService, notificationService, reportingService);

    doAnswer(invocation -> {
      modDetailController = mock(ModDetailController.class);
      when(modDetailController.getRoot()).then(invocation1 -> new Pane());
      return modDetailController;
    }).when(uiService).loadFxml("theme/vault/mod/mod_detail.fxml");

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
  public void testShowModDetail() {
    ModVersion modVersion = ModInfoBeanBuilder.create().defaultValues().get();
    Platform.runLater(()-> instance.onDisplayDetails(modVersion));
    WaitForAsyncUtils.waitForFxEvents();

    verify(modDetailController).setModVersion(modVersion);
    assertThat(modDetailController.getRoot().isVisible(), is(true));
  }
}
