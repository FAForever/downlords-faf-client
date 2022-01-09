package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.test.UITest;
import com.faforever.commons.lobby.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class WatchGameMenuItemTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReplayService replayService;

  private PlayerBean player;
  private WatchGameMenuItem instance;

  @BeforeEach
  public void setUp() {

  }

  @Test
  public void testWatchGame() {
    player = PlayerBeanBuilder.create().defaultValues()
        .game(GameBeanBuilder.create().status(GameStatus.PLAYING).get()).get();

    initializeInstance();
    instance.onClicked(player);

    verify(replayService).runLiveReplay(player.getGame().getId());
  }

  @Test
  public void testRunReplayWithError() {
    player = PlayerBeanBuilder.create().defaultValues()
        .game(GameBeanBuilder.create().defaultValues().status(GameStatus.PLAYING).get()).get();

    Exception e = new RuntimeException();
    doThrow(e).when(replayService).runLiveReplay(player.getGame().getId());

    initializeInstance();
    instance.onClicked(player);

    verify(notificationService).addImmediateErrorNotification(e, "replays.live.loadFailure.message");
  }

  @Test
  public void testVisibleItem() {
    player = PlayerBeanBuilder.create().defaultValues()
        .game(GameBeanBuilder.create().defaultValues().status(GameStatus.PLAYING).get()).get();

    initializeInstance();

    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItem() {
    player = PlayerBeanBuilder.create().defaultValues().game(null).get();

    initializeInstance();

    assertFalse(instance.isVisible());
  }

  private void initializeInstance() {
    instance = new WatchGameMenuItem(i18n, replayService, notificationService);
    instance.setObject(player);
  }
}