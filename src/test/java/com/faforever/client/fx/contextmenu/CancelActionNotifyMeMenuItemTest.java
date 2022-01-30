package com.faforever.client.fx.contextmenu;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.replay.LiveReplayService;
import com.faforever.client.replay.LiveReplayService.LiveReplayAction;
import com.faforever.client.test.UITest;
import javafx.util.Pair;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CancelActionNotifyMeMenuItemTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private LiveReplayService liveReplayService;

  @InjectMocks
  private CancelActionNotifyMeMenuItem instance;

  @Test
  public void testOnClickedCancelActionNotifyMe() {
    instance.onClicked();
    verify(liveReplayService).stopTrackingReplay();
  }

  @Test
  public void testVisibleItem() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(liveReplayService.getTrackingReplay()).thenReturn(Optional.of(new Pair<>(game.getId(), LiveReplayAction.NOTIFY_ME)));

    instance.setObject(game);
    assertTrue(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoGame() {
    instance.setObject(null);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoStartTimeGame() {
    GameBean game = GameBeanBuilder.create().defaultValues().startTime(null).get();
    instance.setObject(game);
    assertFalse(instance.isVisible());
  }

  @Test
  public void testInvisibleItemIfNoTrackingOwnReplay() {
    GameBean game = GameBeanBuilder.create().defaultValues().id(1).get();
    when(liveReplayService.getTrackingReplay()).thenReturn(Optional.of(new Pair<>(2, LiveReplayAction.NOTIFY_ME)));

    instance.setObject(game);
    assertFalse(instance.isVisible());
  }
}