package com.faforever.client.replay;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.GameBean;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.replay.LiveReplayService.LiveReplayAction;
import com.faforever.client.test.UITest;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Pair;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LiveReplayServiceTest extends UITest {

  private static final int WATCH_DELAY = 3; // in seconds;

  @Mock
  private GameService gameService;
  @Mock
  private TaskScheduler taskScheduler;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReplayService replayService;
  @Mock
  private I18n i18n;
  @Spy
  private ClientProperties clientProperties;

  @InjectMocks
  private LiveReplayService instance;
  private final BooleanProperty gameRunningProperty = new SimpleBooleanProperty(false);

  @BeforeEach
  public void setUp() throws Exception {
    clientProperties.getReplay().setWatchDelaySeconds(WATCH_DELAY);
    when(gameService.gameRunningProperty()).thenReturn(gameRunningProperty);

    instance.afterPropertiesSet();
  }

  @Test
  public void testGetWatchDelayTime() {
    GameBean game1 = GameBeanBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();
    assertFalse(instance.getWatchDelayTime(game1).isNegative());

    GameBean game2 = GameBeanBuilder.create().defaultValues().startTime(OffsetDateTime.now().minusSeconds(6)).get();
    assertTrue(instance.getWatchDelayTime(game2).isNegative());
  }

  @Test
  public void testCanWatchReplay() {
    GameBean game = GameBeanBuilder.create().defaultValues().startTime(OffsetDateTime.now().minusSeconds(6)).get();
    assertTrue(instance.canWatchReplay(game));
  }

  @Test
  public void testCannotWatchReplay() {
    GameBean game = GameBeanBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();
    assertFalse(instance.canWatchReplay(game));
  }

  @Test
  public void testNotifyMeWhenReplayIsAvailable() {
    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    GameBean game = GameBeanBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();

    instance.performActionWhenAvailable(game, LiveReplayAction.NOTIFY_ME);

    assertEquals(new Pair<>(game.getId(), LiveReplayAction.NOTIFY_ME), instance.getTrackingReplayProperty().getValue());
    verify(taskScheduler).schedule(captor.capture(), any(Instant.class));
    captor.getValue().run();
    verify(notificationService).addNotification(any(TransientNotification.class));
    assertNull(instance.getTrackingReplayProperty().getValue());
  }

  @Test
  public void testRunReplayWhenIsAvailable() {
    ArgumentCaptor<Runnable> runNotificationCaptor = ArgumentCaptor.forClass(Runnable.class);
    ArgumentCaptor<Runnable> runReplayCaptor = ArgumentCaptor.forClass(Runnable.class);
    GameBean game = GameBeanBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();

    instance.performActionWhenAvailable(game, LiveReplayAction.RUN);

    assertEquals(new Pair<>(game.getId(), LiveReplayAction.RUN), instance.getTrackingReplayProperty().getValue());
    verify(taskScheduler).schedule(runNotificationCaptor.capture(), any(Instant.class));
    runNotificationCaptor.getValue().run();
    verify(notificationService).addNotification(any(TransientNotification.class));

    verify(taskScheduler, times(2)).schedule(runReplayCaptor.capture(), any(Instant.class));
    runReplayCaptor.getValue().run();
    verify(replayService).runLiveReplay(game.getId());
    assertNull(instance.getTrackingReplayProperty().getValue());
  }

  @Test
  public void testGetTrackingReplayProperty() {
    assertNull(instance.getTrackingReplayProperty().getValue());
  }

  @Test
  public void testStopTrackingReplayWhenNoTask() {
    assertNull(instance.getTrackingReplayProperty().getValue());
    instance.stopTrackingReplay();
    assertNull(instance.getTrackingReplayProperty().getValue());
  }

  @Test
  public void testStopTrackingReplayWhenTaskIsScheduled() {
    GameBean game = GameBeanBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();

    ScheduledFuture mockFutureTask = mock(ScheduledFuture.class);
    when(taskScheduler.schedule(any(), any(Instant.class))).thenReturn(mockFutureTask);
    when(mockFutureTask.isCancelled()).thenReturn(false);

    instance.performActionWhenAvailable(game, LiveReplayAction.NOTIFY_ME);
    assertEquals(new Pair<>(game.getId(), LiveReplayAction.NOTIFY_ME), instance.getTrackingReplayProperty().getValue());
    instance.stopTrackingReplay();
    assertNull(instance.getTrackingReplayProperty().getValue());
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testStopTrackingReplayWhenGameStarted() {
    GameBean game = GameBeanBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();

    ScheduledFuture mockFutureTask = mock(ScheduledFuture.class);
    when(taskScheduler.schedule(any(), any(Instant.class))).thenReturn(mockFutureTask);
    when(mockFutureTask.isCancelled()).thenReturn(false);

    instance.performActionWhenAvailable(game, LiveReplayAction.NOTIFY_ME);
    assertEquals(new Pair<>(game.getId(), LiveReplayAction.NOTIFY_ME), instance.getTrackingReplayProperty().getValue());
    gameRunningProperty.set(true);
    assertNull(instance.getTrackingReplayProperty().getValue());
  }
}