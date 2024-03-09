package com.faforever.client.replay;

import com.faforever.client.builders.GameInfoBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.game.GameRunner;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.PlatformTest;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LiveReplayServiceTest extends PlatformTest {

  private static final int WATCH_DELAY = 3; // in seconds;

  @Mock
  private GameService gameService;
  @Mock
  private GameRunner gameRunner;
  @Mock
  private ReplayRunner replayRunner;
  @Mock
  private PlayerService playerService;
  @Mock
  private TaskScheduler taskScheduler;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Spy
  private ClientProperties clientProperties;

  @InjectMocks
  private LiveReplayService instance;
  private BooleanProperty gameRunningProperty;

  @BeforeEach
  public void setUp() throws Exception {
    gameRunningProperty = new SimpleBooleanProperty(false);
    clientProperties.getReplay().setWatchDelaySeconds(WATCH_DELAY);
    when(gameRunner.runningProperty()).thenReturn(gameRunningProperty);

    instance.afterPropertiesSet();
  }

  @Test
  public void testGetWatchDelayTime() {
    GameInfo game1 = GameInfoBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();
    assertFalse(instance.getWatchDelayTime(game1).isNegative());

    GameInfo game2 = GameInfoBuilder.create().defaultValues().startTime(OffsetDateTime.now().minusSeconds(6)).get();
    assertTrue(instance.getWatchDelayTime(game2).isNegative());
  }

  @Test
  public void testCanWatchReplay() {
    GameInfo game = GameInfoBuilder.create().defaultValues().startTime(OffsetDateTime.now().minusSeconds(6)).get();
    assertTrue(instance.canWatchReplay(game));
  }

  @Test
  public void testCannotWatchReplay() {
    GameInfo game = GameInfoBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();
    assertFalse(instance.canWatchReplay(game));
  }

  @Test
  public void testNotifyMeWhenReplayIsAvailable() {
    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    GameInfo game = GameInfoBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();

    instance.performActionWhenAvailable(game, TrackingLiveReplayAction.NOTIFY_ME);

    assertEquals(new TrackingLiveReplay(game.getId(), TrackingLiveReplayAction.NOTIFY_ME), instance.trackingLiveReplayProperty().getValue());
    verify(taskScheduler).schedule(captor.capture(), any(Instant.class));
    captor.getValue().run();
    verify(notificationService).addNotification(any(PersistentNotification.class));
    assertNull(instance.trackingLiveReplayProperty().getValue());
  }

  @Test
  public void testRunReplayWhenIsAvailable() {
    ArgumentCaptor<Runnable> runReplayCaptor = ArgumentCaptor.forClass(Runnable.class);
    GameInfo game = GameInfoBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();

    instance.performActionWhenAvailable(game, TrackingLiveReplayAction.RUN_REPLAY);

    assertEquals(new TrackingLiveReplay(game.getId(), TrackingLiveReplayAction.RUN_REPLAY), instance.trackingLiveReplayProperty().getValue());
    verify(taskScheduler).schedule(runReplayCaptor.capture(), any(Instant.class));
    runReplayCaptor.getValue().run();

    verify(notificationService).addNotification(any(TransientNotification.class));
    verify(replayRunner).runWithLiveReplay(any());
    assertNull(instance.trackingLiveReplayProperty().getValue());
  }

  @Test
  public void testGetTrackingReplayProperty() {
    assertNull(instance.trackingLiveReplayProperty().getValue());
  }

  @Test
  public void testStopTrackingReplayWhenNoTask() {
    assertNull(instance.trackingLiveReplayProperty().getValue());
    instance.stopTrackingLiveReplay();
    assertNull(instance.trackingLiveReplayProperty().getValue());
  }


  @Test
  @SuppressWarnings({"unchecked", "rawtypes", "RedundantExplicitVariableType"})
  public void testStopTrackingReplayWhenTaskIsScheduled() {
    GameInfo game = GameInfoBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();

    ScheduledFuture mockFutureTask = mock(ScheduledFuture.class);
    when(taskScheduler.schedule(any(), any(Instant.class))).thenReturn(mockFutureTask);
    when(mockFutureTask.isCancelled()).thenReturn(false);

    instance.performActionWhenAvailable(game, TrackingLiveReplayAction.NOTIFY_ME);
    assertEquals(new TrackingLiveReplay(game.getId(), TrackingLiveReplayAction.NOTIFY_ME), instance.trackingLiveReplayProperty().getValue());
    instance.stopTrackingLiveReplay();
    assertNull(instance.trackingLiveReplayProperty().getValue());
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked", "RedundantExplicitVariableType"})
  public void testStopTrackingReplayWhenGameStarted() {
    GameInfo game = GameInfoBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();

    ScheduledFuture mockFutureTask = mock(ScheduledFuture.class);
    when(taskScheduler.schedule(any(), any(Instant.class))).thenReturn(mockFutureTask);
    when(mockFutureTask.isCancelled()).thenReturn(false);

    instance.performActionWhenAvailable(game, TrackingLiveReplayAction.NOTIFY_ME);
    assertEquals(new TrackingLiveReplay(game.getId(), TrackingLiveReplayAction.NOTIFY_ME), instance.trackingLiveReplayProperty().getValue());
    gameRunningProperty.set(true);
    assertNull(instance.trackingLiveReplayProperty().getValue());
  }
}