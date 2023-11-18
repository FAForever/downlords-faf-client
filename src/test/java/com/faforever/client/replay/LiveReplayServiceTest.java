package com.faforever.client.replay;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
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

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LiveReplayServiceTest extends PlatformTest {

  private static final int WATCH_DELAY = 3; // in seconds;

  @Mock
  private GameService gameService;
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
  public void testRunLiveReplay() throws Exception {
    when(gameService.runWithLiveReplay(any(URI.class), anyInt(), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));

    instance.runLiveReplay(new URI("faflive://example.com/123/456.scfareplay?mod=faf&map=map%20name"));

    verify(gameService).runWithLiveReplay(new URI("gpgnet://example.com/123/456.scfareplay"), 123, "faf", "map name");
  }

  @Test
  public void testNotifyMeWhenReplayIsAvailable() {
    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    GameBean game = GameBeanBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();

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
    GameBean game = GameBeanBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();

    when(gameService.runWithLiveReplay(any(URI.class), anyInt(), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));
    when(gameService.getByUid(game.getId())).thenReturn(game);
    when(playerService.getCurrentPlayer()).thenReturn(player);
    instance.performActionWhenAvailable(game, TrackingLiveReplayAction.RUN_REPLAY);

    assertEquals(new TrackingLiveReplay(game.getId(), TrackingLiveReplayAction.RUN_REPLAY), instance.trackingLiveReplayProperty().getValue());
    verify(taskScheduler).schedule(runReplayCaptor.capture(), any(Instant.class));
    runReplayCaptor.getValue().run();

    verify(notificationService).addNotification(any(TransientNotification.class));
    verify(gameService).runWithLiveReplay(any(URI.class), anyInt(), anyString(), anyString());
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
    GameBean game = GameBeanBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();

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
    GameBean game = GameBeanBuilder.create().defaultValues().startTime(OffsetDateTime.now()).get();

    ScheduledFuture mockFutureTask = mock(ScheduledFuture.class);
    when(taskScheduler.schedule(any(), any(Instant.class))).thenReturn(mockFutureTask);
    when(mockFutureTask.isCancelled()).thenReturn(false);

    instance.performActionWhenAvailable(game, TrackingLiveReplayAction.NOTIFY_ME);
    assertEquals(new TrackingLiveReplay(game.getId(), TrackingLiveReplayAction.NOTIFY_ME), instance.trackingLiveReplayProperty().getValue());
    when(gameService.isGameRunning()).thenReturn(true);
    gameRunningProperty.set(true);
    assertNull(instance.trackingLiveReplayProperty().getValue());
  }
}