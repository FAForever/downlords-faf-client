package com.faforever.client.leaderboard;

import com.faforever.client.builders.LeagueEntryBeanBuilder;
import com.faforever.client.builders.SubdivisionBeanBuilder;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SubDivisionTabControllerTest extends UITest {

  private SubDivisionTabController instance;
  @Mock
  private I18n i18n;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private UiService uiService;

  @BeforeEach
  public void setUp() throws Exception {


    instance = new SubDivisionTabController(leaderboardService, notificationService, i18n, uiService);

    loadFxml("theme/leaderboard/subDivisionTab.fxml", clazz -> instance);
    instance.initialize();
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
    assertEquals(leagueEntryBean1, instance.ratingTable.getItems().get(0));
    assertEquals(11, leagueEntryBean1.getRank());
    assertEquals(12, leagueEntryBean2.getRank());
  }
}
