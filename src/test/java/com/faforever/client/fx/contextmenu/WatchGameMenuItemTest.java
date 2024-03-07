package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.GameInfoBuilder;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayRunner;
import com.faforever.client.test.PlatformTest;
import com.faforever.commons.lobby.GameStatus;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
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
  private ReplayRunner replayRunner;

  @InjectMocks
  private WatchGameMenuItem instance;

  @Test
  public void testWatchGame() {
    PlayerInfo player = PlayerInfoBuilder.create()
                                         .defaultValues()
                                         .game(GameInfoBuilder.create().status(GameStatus.PLAYING).get())
                                         .get();
    instance.setObject(player);
    instance.onClicked();

    verify(replayRunner).runWithLiveReplay(player.getGame());
  }

  @Test
  public void testRunReplayWithError() {
    PlayerInfo player = PlayerInfoBuilder.create()
                                         .defaultValues()
                                         .game(
                                             GameInfoBuilder.create().defaultValues().status(GameStatus.PLAYING).get())
                                         .get();
    Exception e = new RuntimeException();
    doThrow(e).when(replayRunner).runWithLiveReplay(player.getGame());

    instance.setObject(player);
    instance.onClicked();

    verify(notificationService).addImmediateErrorNotification(e, "replays.live.loadFailure.message");
  }

  @Test
  public void testVisibleItemIfPlayerIsPlaying() {
    instance.setObject(PlayerInfoBuilder.create()
                                        .defaultValues()
                                        .game(GameInfoBuilder.create().defaultValues().status(GameStatus.PLAYING).get())
                                        .get());

    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfPlayerIsIdle() {
    instance.setObject(PlayerInfoBuilder.create().defaultValues().game(null).get());

    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoPlayer() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }
}