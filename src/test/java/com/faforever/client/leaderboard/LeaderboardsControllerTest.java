package com.faforever.client.leaderboard;

import com.faforever.client.builders.LeagueBeanBuilder;
import com.faforever.client.builders.LeagueSeasonBeanBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

public class LeaderboardsControllerTest extends PlatformTest {

  @InjectMocks
  private LeaderboardsController instance;

  @Mock
  private I18n i18n;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private UiService uiService;
  @Mock
  private NavigationHandler navigationHandler;

  @Mock
  private LeaderboardController controller;

  @BeforeEach
  public void setUp() throws Exception {
    when(leaderboardService.getLeagues()).thenReturn(Flux.just(LeagueBeanBuilder.create().defaultValues().get()));
    when(leaderboardService.getLatestSeason(any())).thenReturn(
        Mono.just(LeagueSeasonBeanBuilder.create().defaultValues().get()));
    when(i18n.getOrDefault(anyString(), anyString())).thenReturn("league");
    when(uiService.loadFxml("theme/leaderboard/leaderboard.fxml")).thenReturn(controller);
    when(controller.getRoot()).thenReturn(new StackPane());

    loadFxml("theme/leaderboard/leaderboards.fxml", clazz -> instance);
  }

  @Test
  public void testInitialize() {
    waitForFxEvents();

    assertEquals(1, instance.navigationBox.getChildren().size());
  }

  @Test
  public void testNoLeagues() {
    when(leaderboardService.getLeagues()).thenReturn(Flux.empty());

    reinitialize(instance);

    verify(notificationService).addImmediateErrorNotification(any(), eq("leaderboard.failedToLoadLeaderboards"));
  }

  @Test
  public void testInitializeWithLeagueError() {
    when(leaderboardService.getLeagues()).thenReturn(Flux.error(new FakeTestException()));

    reinitialize(instance);

    verify(notificationService).addImmediateErrorNotification(any(), eq("leaderboard.failedToLoadLeaderboards"));
  }

  @Test
  public void testInitializeWithSeasonError() {
    when(leaderboardService.getLatestSeason(any())).thenReturn(Mono.error(new FakeTestException()));

    reinitialize(instance);

    verify(notificationService).addImmediateErrorNotification(any(), eq("leaderboard.failedToLoadLeaderboards"));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.getRoot(), instance.leaderboardRoot);
    assertNull(instance.getRoot().getParent());
  }
}
