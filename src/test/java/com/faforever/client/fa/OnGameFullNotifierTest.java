package com.faforever.client.fa;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.relay.event.GameFullEvent;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameBuilder;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.test.ServiceTest;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class OnGameFullNotifierTest extends ServiceTest {
  private OnGameFullNotifier instance;
  @Mock
  private EventBus eventBus;
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

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new OnGameFullNotifier(platformService, executorService, notificationService, i18n,
        mapService, eventBus, gameService, new ClientProperties());
    instance.afterPropertiesSet();

    doAnswer(invocation -> {
      ((Runnable) invocation.getArgument(0)).run();
      return null;
    }).when(executorService).execute(any(Runnable.class));

    verify(eventBus).register(instance);
  }

  @Test
  public void testOnGameFull() throws Exception {
    Game game = GameBuilder.create().defaultValues().get();
    when(gameService.getCurrentGame()).thenReturn(game);
    when(platformService.isWindowFocused("Forged Alliance")).thenReturn(false);

    CountDownLatch countDownLatch = new CountDownLatch(1);
    doAnswer(invocation -> {
      countDownLatch.countDown();
      return null;
    }).when(platformService).stopFlashingWindow("Forged Alliance");

    instance.onGameFull(new GameFullEvent());

    verify(notificationService).addNotification(any(TransientNotification.class));
    verify(executorService).execute(any(Runnable.class));
  }

  @Test
  public void testAlreadyFocusedDoesntTriggerNotification() throws Exception {
    Game game = GameBuilder.create().defaultValues().get();
    when(gameService.getCurrentGame()).thenReturn(game);
    when(platformService.isWindowFocused("Forged Alliance")).thenReturn(true);

    instance.onGameFull(new GameFullEvent());

    verifyZeroInteractions(notificationService);
  }
}
