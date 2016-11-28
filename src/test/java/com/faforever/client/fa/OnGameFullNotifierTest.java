package com.faforever.client.fa;

import com.faforever.client.fa.relay.event.GameFullEvent;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameBuilder;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class OnGameFullNotifierTest {
  private OnGameFullNotifier instance;
  @Mock
  private EventBus eventBus;
  @Mock
  private ThreadPoolExecutor threadPoolExecutor;
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

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new OnGameFullNotifier();
    instance.eventBus = eventBus;
    instance.threadPoolExecutor = threadPoolExecutor;
    instance.gameService = gameService;
    instance.i18n = i18n;
    instance.notificationService = notificationService;
    instance.mapService = mapService;
    instance.platformService = platformService;
    instance.faWindowTitle = "Forged Alliance";
    instance.postConstruct();

    doAnswer(invocation -> {
      invocation.getArgumentAt(0, Runnable.class).run();
      return null;
    }).when(threadPoolExecutor).submit(any(Runnable.class));

    verify(eventBus).register(instance);
  }

  @Test
  public void testOnGameFull() throws Exception {
    Game game = GameBuilder.create().defaultValues().get();
    when(gameService.getCurrentGame()).thenReturn(game);
    when(platformService.getForegroundWindowTitle()).thenReturn("Some window");

    CountDownLatch countDownLatch = new CountDownLatch(1);
    doAnswer(invocation -> {
      countDownLatch.countDown();
      return null;
    }).when(platformService).stopFlashingWindow("Forged Alliance");

    instance.onGameFull(new GameFullEvent());

    verify(notificationService).addNotification(any(TransientNotification.class));
    verify(threadPoolExecutor).submit(any(Runnable.class));
    verify(platformService).startFlashingWindow("Forged Alliance");
    verify(platformService).stopFlashingWindow("Forged Alliance");
  }

  @Test
  public void testAlreadyFocusedDoesntTriggerNotification() throws Exception {
    Game game = GameBuilder.create().defaultValues().get();
    when(gameService.getCurrentGame()).thenReturn(game);
    when(platformService.getForegroundWindowTitle()).thenReturn("Forged Alliance");

    instance.onGameFull(new GameFullEvent());

    verifyZeroInteractions(notificationService);
  }
}
