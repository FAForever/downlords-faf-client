package com.faforever.client.fa;

import com.faforever.client.game.GameRunner;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.lobby.HostGameGpgCommand;
import com.faforever.commons.lobby.JoinGameGpgCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.scheduling.TaskScheduler;
import reactor.test.publisher.TestPublisher;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class GameLobbyWatchdogTest extends ServiceTest {
  @InjectMocks
  private GameLobbyWatchdog instance;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private GameRunner gameRunner;
  @Mock
  private TaskScheduler taskScheduler;

  private final TestPublisher<HostGameGpgCommand> hostGamePublisher = TestPublisher.create();
  private final TestPublisher<JoinGameGpgCommand> joinGamePublisher = TestPublisher.create();
  private final ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);

  @BeforeEach
  public void setup() {
    when(fafServerAccessor.getEvents(HostGameGpgCommand.class)).thenReturn(hostGamePublisher.flux());
    when(fafServerAccessor.getEvents(JoinGameGpgCommand.class)).thenReturn(joinGamePublisher.flux());
    lenient().doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    lenient().when(i18n.get(any())).thenReturn("text");

    instance.afterPropertiesSet();
  }

  private Runnable enterLobbyAndCaptureTimeout() {
    instance.onGameEnteredLobby();
    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(taskScheduler).schedule(captor.capture(), any(Instant.class));
    return captor.getValue();
  }

  @Test
  public void testNotifiesWhenServerDoesNotRespond() {
    when(gameRunner.isRunning()).thenReturn(true);

    enterLobbyAndCaptureTimeout().run();

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);
    verify(notificationService).addNotification(captor.capture());

    captor.getValue().actions().getFirst().run();
    verify(gameRunner).killGame();
  }

  @Test
  public void testDoesNotNotifyWhenGameIsNoLongerRunning() {
    when(gameRunner.isRunning()).thenReturn(false);

    enterLobbyAndCaptureTimeout().run();

    verifyNoInteractions(notificationService);
  }

  @Test
  public void testJoinGameCommandCancelsTimeout() {
    instance.onGameEnteredLobby();
    verify(scheduledFuture, never()).cancel(false);

    joinGamePublisher.next(mock(JoinGameGpgCommand.class));

    verify(scheduledFuture).cancel(false);
  }

  @Test
  public void testHostGameCommandCancelsTimeout() {
    instance.onGameEnteredLobby();

    hostGamePublisher.next(mock(HostGameGpgCommand.class));

    verify(scheduledFuture).cancel(false);
  }
}
