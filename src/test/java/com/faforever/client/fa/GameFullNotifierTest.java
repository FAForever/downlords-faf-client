package com.faforever.client.fa;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.ForgedAlliance;
import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.test.ServiceTest;
import javafx.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class GameFullNotifierTest extends ServiceTest {
  @InjectMocks
  private GameFullNotifier instance;
  @Mock
  private ExecutorService executorService;
  @Mock
  private GameService gameService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private MapService mapService;
  @Mock
  private PlatformService platformService;
  @Mock
  private ClientProperties clientProperties;

  private ForgedAlliance forgedAlliance;

  @BeforeEach
  public void setUp() throws Exception {
    forgedAlliance = new ForgedAlliance();
    when(clientProperties.getForgedAlliance()).thenReturn(forgedAlliance);

    instance.afterPropertiesSet();
  }

  @Test
  public void testNotifyUserWhenGameLobbyWindowIsNotFocused() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(gameService.getCurrentGame()).thenReturn(game);
    when(gameService.getRunningProcessId()).thenReturn(1L);
    when(platformService.getFocusedWindowProcessId()).thenReturn(2L);

    instance.onGameFull();
    verify(notificationService).addNotification(any(TransientNotification.class));
  }

  @Test
  public void testDoNotNotifyUserWhenGameLobbyWindowIsFocused() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(gameService.getCurrentGame()).thenReturn(game);
    when(gameService.getRunningProcessId()).thenReturn(1L);
    when(platformService.getFocusedWindowProcessId()).thenReturn(1L);

    instance.onGameFull();
    verifyNoInteractions(notificationService);
  }

  @Test
  public void testFocusToFaWindowFromNonFaWindow() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(gameService.getCurrentGame()).thenReturn(game);
    when(gameService.getRunningProcessId()).thenReturn(1L);
    when(platformService.getFocusedWindowProcessId()).thenReturn(2L);

    instance.onGameFull();
    ArgumentCaptor<TransientNotification> argumentCaptor = ArgumentCaptor.forClass(TransientNotification.class);
    verify(notificationService).addNotification(argumentCaptor.capture());

    argumentCaptor.getValue().getActionCallback().call(any(Event.class));
    verify(platformService).focusWindow(forgedAlliance.getWindowTitle(), 1L);
    verify(platformService, never()).minimizeFocusedWindow();
  }

  @Test
  public void testFocusToFaWindowFromAnotherFaWindow() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(gameService.getCurrentGame()).thenReturn(game);
    when(gameService.getRunningProcessId()).thenReturn(1L);
    when(platformService.getFocusedWindowProcessId()).thenReturn(2L);
    when(platformService.isWindowFocused(forgedAlliance.getWindowTitle())).thenReturn(true);

    instance.onGameFull();
    ArgumentCaptor<TransientNotification> argumentCaptor = ArgumentCaptor.forClass(TransientNotification.class);
    verify(notificationService).addNotification(argumentCaptor.capture());

    argumentCaptor.getValue().getActionCallback().call(any(Event.class));
    verify(platformService).minimizeFocusedWindow();
    verify(platformService).focusWindow(forgedAlliance.getWindowTitle(), 1L);
  }

  @Test
  public void testStartFlashingFaWindow() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(gameService.getCurrentGame()).thenReturn(game);
    when(gameService.getRunningProcessId()).thenReturn(1L);
    when(platformService.getFocusedWindowProcessId()).thenReturn(2L);

    instance.onGameFull();
    ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(executorService).execute(argumentCaptor.capture());

    argumentCaptor.getValue().run();
    verify(platformService).startFlashingWindow(forgedAlliance.getWindowTitle(), 1L);
  }

  @Test
  public void testStopFlashingFaWindow() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(gameService.getCurrentGame()).thenReturn(game);
    when(gameService.getRunningProcessId()).thenReturn(1L);
    when(platformService.getFocusedWindowProcessId()).thenReturn(2L);

    instance.onGameFull();
    ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(executorService).execute(argumentCaptor.capture());

    when(gameService.isGameRunning()).thenReturn(true, true, true, true);
    when(platformService.isWindowFocused(forgedAlliance.getWindowTitle(), 1L)).thenReturn(false, false, false, true); // delay imitation

    argumentCaptor.getValue().run();
    verify(platformService).stopFlashingWindow(forgedAlliance.getWindowTitle(), 1L);
  }
}
