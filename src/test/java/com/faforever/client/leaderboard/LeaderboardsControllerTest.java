package com.faforever.client.leaderboard;

import com.faforever.client.builders.LeagueBeanBuilder;
import com.faforever.client.builders.LeagueSeasonBeanBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class LeaderboardsControllerTest extends UITest {

  private LeaderboardsController instance;

  @Mock
  private EventBus eventBus;
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
    when(leaderboardService.getLeagues()).thenReturn(
        CompletableFuture.completedFuture(List.of(LeagueBeanBuilder.create().defaultValues().get())));
    when(leaderboardService.getLatestSeason(anyInt())).thenReturn(
        CompletableFuture.completedFuture(LeagueSeasonBeanBuilder.create().defaultValues().get()));

    instance = new LeaderboardsController(eventBus, i18n, leaderboardService, notificationService, uiService);

    loadFxml("theme/leaderboard/leaderboards.fxml", clazz -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.getRoot(), instance.leaderboardRoot);
    assertNull(instance.getRoot().getParent());
  }
}
