package com.faforever.client.leaderboard;

import com.faforever.client.builders.LeagueEntryBeanBuilder;
import com.faforever.client.builders.SubdivisionBeanBuilder;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.PlatformTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SubDivisionTabControllerTest extends PlatformTest {

  @Mock
  private I18n i18n;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;

  @InjectMocks
  private SubDivisionTabController instance;

  @BeforeEach
  public void setUp() throws Exception {
    loadFxml("theme/leaderboard/sub_division_tab.fxml", clazz -> instance);
    reinitialize(instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.getRoot(), instance.subDivisionTab);
  }

  @Test
  public void testPopulate() {
    SubdivisionBean subdivisionBean = SubdivisionBeanBuilder.create().defaultValues().get();
    LeagueEntryBean leagueEntryBean1 = LeagueEntryBeanBuilder.create().defaultValues().id(2).subdivision(subdivisionBean).get();
    LeagueEntryBean leagueEntryBean2 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean).get();
    when(leaderboardService.getEntries(subdivisionBean)).thenReturn(CompletableFuture.completedFuture(List.of(
        leagueEntryBean1, leagueEntryBean2)));
    when(leaderboardService.getPlayerNumberInHigherDivisions(any())).thenReturn(CompletableFuture.completedFuture(10));

    instance.populate(subdivisionBean);
    assertEquals(2, instance.ratingTable.getItems().size());
    assertEquals(leagueEntryBean1, instance.ratingTable.getItems().getFirst());
    assertEquals(11, leagueEntryBean1.getRank());
    assertEquals(12, leagueEntryBean2.getRank());
  }

  @Test
  public void testPopulateWithError() {
    SubdivisionBean subdivisionBean = SubdivisionBeanBuilder.create().defaultValues().get();
    when(leaderboardService.getEntries(subdivisionBean)).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    instance.populate(subdivisionBean);

    verify(notificationService).addImmediateErrorNotification(any(CompletionException.class), eq("leaderboard.failedToLoad"));
  }
}
