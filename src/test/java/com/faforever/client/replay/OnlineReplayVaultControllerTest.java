package com.faforever.client.replay;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenOnlineReplayVaultEvent;
import com.faforever.client.main.event.ShowReplayEvent;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.CategoryFilterController;
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
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OnlineReplayVaultControllerTest extends AbstractPlainJavaFxTest {
  private OnlineReplayVaultController instance;

  @Mock
  private ModService modService;
  @Mock
  private ReplayService replayService;
  @Mock
  private UiService uiService;
  @Mock
  private ReplayDetailController replayDetailController;
  @Mock
  private SpecificationController specificationController;
  @Mock
  private LogicalNodeController logicalNodeController;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private SearchController searchController;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private CategoryFilterController categoryFilterController;

  @Captor
  private ArgumentCaptor<Consumer<SearchConfig>> searchListenerCaptor;
  private SortConfig sortOrder;
  private SearchConfig standardSearchConfig;
  private final Replay testReplay = new Replay();

  @Before
  public void setUp() throws Exception {
    when(replayDetailController.getRoot()).thenReturn(new Pane());

    when(uiService.loadFxml("theme/vault/replay/replay_detail.fxml")).thenAnswer(invocation -> replayDetailController);

    when(modService.getFeaturedMods()).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    when(replayService.getNewestReplaysWithPageCount(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), 0)));
    when(replayService.getHighestRatedReplaysWithPageCount(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), 0)));
    when(replayService.findById(anyInt())).thenReturn(CompletableFuture.completedFuture(Optional.of(testReplay)));
    when(replayService.getOwnReplaysWithPageCount(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), 0)));
    when(preferencesService.getPreferences()).thenReturn(new Preferences());
    when(uiService.loadFxml("theme/vault/search/categoryFilter.fxml")).thenReturn(categoryFilterController);
    when(i18n.get(anyString())).thenReturn("test");

    sortOrder = preferencesService.getPreferences().getVaultPrefs().getOnlineReplaySortConfig();
    standardSearchConfig = new SearchConfig(sortOrder, "query");

    instance = new OnlineReplayVaultController(modService, replayService, uiService, notificationService, i18n, preferencesService, reportingService);

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

    verify(searchController).setSearchListener(searchListenerCaptor.capture());
  }

  @Test
  public void testSetSupplier() {
    instance.searchType = SearchType.SEARCH;
    instance.setSupplier(standardSearchConfig);
    instance.searchType = SearchType.NEWEST;
    instance.setSupplier(null);
    instance.searchType = SearchType.OWN;
    instance.setSupplier(null);
    instance.searchType = SearchType.HIGHEST_RATED;
    instance.setSupplier(null);
    instance.searchType = SearchType.PLAYER;
    instance.setSupplier(null);

    verify(replayService).findByQueryWithPageCount(standardSearchConfig.getSearchQuery(), instance.pageSize, 1, standardSearchConfig.getSortConfig());
    verify(replayService).getNewestReplaysWithPageCount(instance.pageSize, 1);
    verify(replayService).getHighestRatedReplaysWithPageCount(instance.pageSize, 1);
    verify(replayService).getOwnReplaysWithPageCount(instance.pageSize, 1);
    verify(replayService).getReplaysForPlayerWithPageCount(0, instance.pageSize, 1, standardSearchConfig.getSortConfig());
  }

  @Test
  public void testShowReplayEventWhenUninitialized() {
    Platform.runLater(() -> instance.display(new ShowReplayEvent(123)));
    WaitForAsyncUtils.waitForFxEvents();
    verify(replayDetailController).setReplay(testReplay);
  }

  @Test
  public void testShowReplayEventWhenInitialized() {
    Platform.runLater(() -> instance.display(new OpenOnlineReplayVaultEvent()));
    Platform.runLater(() -> instance.display(new ShowReplayEvent(123)));
    WaitForAsyncUtils.waitForFxEvents();
    verify(replayDetailController).setReplay(testReplay);
  }

  @Test
  public void showReplayButReplayNotPresent() {
    when(replayService.findById(anyInt())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
    Platform.runLater(() -> instance.display(new ShowReplayEvent(123)));
    WaitForAsyncUtils.waitForFxEvents();
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }
}
