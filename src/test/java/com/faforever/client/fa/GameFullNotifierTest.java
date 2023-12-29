package com.faforever.client.fa;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameRunner;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class GameFullNotifierTest extends ServiceTest {
  @InjectMocks
  private GameFullNotifier instance;
  @Mock
  private GameRunner gameRunner;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private MapService mapService;
  @Mock
  private PlatformService platformService;
  @Spy
  private ClientProperties clientProperties;

  @BeforeEach
  public void setup() {
    when(gameRunner.isRunning()).thenReturn(true);
  }

  @Test
  public void testNotifyUserWhenGameLobbyWindowIsNotFocused() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(gameRunner.getRunningGame()).thenReturn(game);
    when(gameRunner.getRunningProcessId()).thenReturn(1L);
    when(platformService.getFocusedWindowProcessId()).thenReturn(2L);

    instance.onGameFull();
    verify(notificationService).addNotification(any(TransientNotification.class));
  }

  @Test
  public void testDoNotNotifyUserWhenGameLobbyWindowIsFocused() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(gameRunner.getRunningGame()).thenReturn(game);
    when(gameRunner.getRunningProcessId()).thenReturn(1L);
    when(platformService.getFocusedWindowProcessId()).thenReturn(1L);

    instance.onGameFull();
    verifyNoInteractions(notificationService);
  }

  @Test
  public void testFocusToFaWindowFromNonFaWindow() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(gameRunner.getRunningGame()).thenReturn(game);
    when(gameRunner.getRunningProcessId()).thenReturn(1L);
    when(platformService.getFocusedWindowProcessId()).thenReturn(2L);

    instance.onGameFull();
    ArgumentCaptor<TransientNotification> argumentCaptor = ArgumentCaptor.forClass(TransientNotification.class);
    verify(notificationService).addNotification(argumentCaptor.capture());

    argumentCaptor.getValue().onAction().run();
    verify(platformService).focusWindow(clientProperties.getForgedAlliance().getWindowTitle(), 1L);
    verify(platformService, never()).minimizeFocusedWindow();
  }

  @Test
  public void testFocusToFaWindowFromAnotherFaWindow() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(gameRunner.getRunningGame()).thenReturn(game);
    when(gameRunner.getRunningProcessId()).thenReturn(1L);
    when(platformService.getFocusedWindowProcessId()).thenReturn(2L);
    when(platformService.isWindowFocused(clientProperties.getForgedAlliance().getWindowTitle())).thenReturn(true);

    instance.onGameFull();
    ArgumentCaptor<TransientNotification> argumentCaptor = ArgumentCaptor.forClass(TransientNotification.class);
    verify(notificationService).addNotification(argumentCaptor.capture());

    argumentCaptor.getValue().onAction().run();
    verify(platformService).minimizeFocusedWindow();
    verify(platformService).focusWindow(clientProperties.getForgedAlliance().getWindowTitle(), 1L);
  }

  @Test
  public void testStartFlashingFaWindow() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(gameRunner.getRunningGame()).thenReturn(game);
    when(gameRunner.getRunningProcessId()).thenReturn(1L);
    when(platformService.getFocusedWindowProcessId()).thenReturn(2L);

    instance.onGameFull();

    verify(platformService).startFlashingWindow(clientProperties.getForgedAlliance().getWindowTitle(), 1L);
  }

  @Test
  public void testStopFlashingFaWindow() throws Exception {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(gameRunner.getRunningGame()).thenReturn(game);
    when(gameRunner.getRunningProcessId()).thenReturn(1L);
    when(platformService.getFocusedWindowProcessId()).thenReturn(2L);

    instance.onGameFull();

    when(platformService.isWindowFocused(clientProperties.getForgedAlliance().getWindowTitle(), 1L)).thenReturn(
        true); // delay imitation

    Thread.sleep(600);
    verify(platformService).stopFlashingWindow(clientProperties.getForgedAlliance().getWindowTitle(), 1L);
  }
}
