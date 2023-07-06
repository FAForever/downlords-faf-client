package com.faforever.client.leaderboard;

import com.faforever.client.builders.LeagueBeanBuilder;
import com.faforever.client.builders.LeagueSeasonBeanBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenLeaderboardEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.scene.control.Tab;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
  private EventBus eventBus;
  @Mock
  private I18n i18n;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private UiService uiService;

  @Mock
  private LeaderboardController controller;

  @BeforeEach
  public void setUp() throws Exception {
    when(leaderboardService.getLeagues()).thenReturn(
        CompletableFuture.completedFuture(List.of(LeagueBeanBuilder.create().defaultValues().get())));
    when(leaderboardService.getLatestSeason(any())).thenReturn(
        CompletableFuture.completedFuture(LeagueSeasonBeanBuilder.create().defaultValues().get()));
    when(i18n.getOrDefault(anyString(), anyString())).thenReturn("league");
    when(uiService.loadFxml("theme/leaderboard/leaderboard.fxml")).thenReturn(controller);
    Tab tab = new Tab();
    when(controller.getRoot()).thenReturn(tab);

    loadFxml("theme/leaderboard/leaderboards.fxml", clazz -> instance);
  }

  @Test
  public void testInitialize() {
    waitForFxEvents();

    assertEquals("league", controller.getRoot().getText());
    assertEquals(1, instance.leaderboardRoot.getTabs().size());
    assertEquals(1, instance.controllers.size());
    assertEquals(controller, instance.controllers.get(0));
  }

  @Test
  public void testNoLeagues() {
    when(leaderboardService.getLeagues()).thenReturn(CompletableFuture.completedFuture(List.of()));

    instance.initialize();

    verify(notificationService).addImmediateWarnNotification(eq("leaderboard.noLeaderboards"));
  }

  @Test
  public void testInitializeWithLeagueError() {
    when(leaderboardService.getLeagues()).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    instance.initialize();

    verify(notificationService).addImmediateErrorNotification(any(CompletionException.class), eq("leaderboard.failedToLoadLeaderboards"));
  }

  @Test
  public void testInitializeWithSeasonError() {
    when(leaderboardService.getLatestSeason(any())).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    instance.initialize();

    verify(notificationService).addImmediateErrorNotification(any(CompletionException.class), eq("leaderboard.failedToLoadLeaderboards"));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.getRoot(), instance.leaderboardRoot);
    assertNull(instance.getRoot().getParent());
  }

  @Test
  public void testOnDisplay() {
    instance.onDisplay(new OpenLeaderboardEvent(controller.getRoot()));

    assertEquals(instance.leaderboardRoot.getSelectionModel().getSelectedItem(), controller.getRoot());
  }
}
