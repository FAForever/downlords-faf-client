package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.LiveReplayService;
import com.faforever.client.test.PlatformTest;
import com.faforever.commons.lobby.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class WatchGameMenuItemTest extends PlatformTest {

  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;
  @Mock
  private LiveReplayService liveReplayService;

  private WatchGameMenuItem instance;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new WatchGameMenuItem(i18n, liveReplayService, notificationService);
  }

  @Test
  public void testWatchGame() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues()
        .game(GameBeanBuilder.create().status(GameStatus.PLAYING).get()).get();
    instance.setObject(player);
    instance.onClicked();

    verify(liveReplayService).runLiveReplay(player.getGame().getId());
  }

  @Test
  public void testRunReplayWithError() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues()
        .game(GameBeanBuilder.create().defaultValues().status(GameStatus.PLAYING).get()).get();
    Exception e = new RuntimeException();
    doThrow(e).when(liveReplayService).runLiveReplay(player.getGame().getId());

    instance.setObject(player);
    instance.onClicked();

    verify(notificationService).addImmediateErrorNotification(e, "replays.live.loadFailure.message");
  }

  @Test
  public void testVisibleItemIfPlayerIsPlaying() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues()
        .game(GameBeanBuilder.create().defaultValues().status(GameStatus.PLAYING).get()).get());

    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfPlayerIsIdle() {
    instance.setObject(PlayerBeanBuilder.create().defaultValues().game(null).get());

    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }
}