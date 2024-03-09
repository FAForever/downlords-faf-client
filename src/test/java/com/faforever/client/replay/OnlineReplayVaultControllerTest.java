package com.faforever.client.replay;

import com.faforever.client.domain.api.Replay;
import com.faforever.client.featuredmod.FeaturedModService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.main.event.OpenOnlineReplayVaultEvent;
import com.faforever.client.main.event.ShowReplayEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.VaultPrefs;
import com.faforever.client.query.CategoryFilterController;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.VaultEntityController.SearchType;
import com.faforever.client.vault.VaultEntityShowRoomController;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import javafx.scene.layout.Pane;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OnlineReplayVaultControllerTest extends PlatformTest {

  @InjectMocks
  private OnlineReplayVaultController instance;

  @Mock
  private FeaturedModService featuredModService;
  @Mock
  private LeaderboardService leaderboardService;
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
  private ReplayCardController replayCardController;

  @Mock
  private ReportingService reportingService;
  @Mock
  private CategoryFilterController categoryFilterController;
  @Mock
  private VaultEntityShowRoomController vaultEntityShowRoomController;
  @Spy
  private VaultPrefs vaultPrefs;

  @Captor
  private ArgumentCaptor<Consumer<SearchConfig>> searchListenerCaptor;
  private SortConfig sortOrder;
  private SearchConfig standardSearchConfig;
  private final Replay testReplay = Instancio.create(Replay.class);

  @BeforeEach
  public void setUp() throws Exception {
    lenient().when(replayDetailController.getRoot()).thenReturn(new Pane());

    lenient().when(featuredModService.getFeaturedMods()).thenReturn(Flux.empty());
    lenient().when(leaderboardService.getLeaderboards()).thenReturn(Flux.empty());
    lenient().when(replayService.getNewestReplaysWithPageCount(anyInt(), anyInt()))
             .thenReturn(Mono.zip(Mono.just(List.<Replay>of()), Mono.just(0)));
    lenient().when(replayService.getHighestRatedReplaysWithPageCount(anyInt(), anyInt()))
             .thenReturn(Mono.zip(Mono.just(List.<Replay>of()), Mono.just(0)));
    lenient().when(replayService.findById(anyInt())).thenReturn(Mono.just(testReplay));
    lenient().when(replayService.getOwnReplaysWithPageCount(anyInt(), anyInt()))
             .thenReturn(Mono.zip(Mono.just(List.<Replay>of()), Mono.just(0)));
    lenient().when(uiService.loadFxml("theme/vault/replay/replay_detail.fxml")).thenReturn(replayDetailController);
    lenient().when(uiService.loadFxml("theme/vault/vault_entity_show_room.fxml"))
             .thenReturn(vaultEntityShowRoomController);
    lenient().when(i18n.get(anyString())).thenReturn("test");
    lenient().when(searchController.addCategoryFilter(any(), any(), anyMap())).thenReturn(categoryFilterController);
    lenient().when(searchController.addCategoryFilter(any(), any(), anyList())).thenReturn(categoryFilterController);

    sortOrder = new VaultPrefs().getOnlineReplaySortConfig();
    standardSearchConfig = new SearchConfig(sortOrder, "query");

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

    verify(replayService).findByQueryWithPageCount(standardSearchConfig, instance.pageSize, 1);
    verify(replayService).getNewestReplaysWithPageCount(instance.pageSize, 1);
    verify(replayService).getHighestRatedReplaysWithPageCount(instance.pageSize, 1);
    verify(replayService).getOwnReplaysWithPageCount(instance.pageSize, 1);
    verify(replayService).getReplaysForPlayerWithPageCount(0, instance.pageSize, 1);
  }

  @Test
  public void testShowReplayEventWhenUninitialized() {
    runOnFxThreadAndWait(() -> instance.display(new ShowReplayEvent(123)));
    verify(replayDetailController).setReplay(testReplay);
  }

  @Test
  public void testShowReplayEventWhenInitialized() {
    runOnFxThreadAndWait(() -> instance.display(new OpenOnlineReplayVaultEvent()));
    runOnFxThreadAndWait(() -> instance.display(new ShowReplayEvent(123)));
    WaitForAsyncUtils.waitForFxEvents();
    verify(replayDetailController).setReplay(testReplay);
  }

  @Test
  public void showReplayButReplayNotPresent() {
    when(replayService.findById(anyInt())).thenReturn(Mono.empty());
    runOnFxThreadAndWait(() -> instance.display(new ShowReplayEvent(123)));
    verify(notificationService).addImmediateWarnNotification(anyString(), anyInt());
  }
}
