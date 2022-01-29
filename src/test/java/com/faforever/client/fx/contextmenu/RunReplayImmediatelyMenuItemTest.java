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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RunReplayImmediatelyMenuItemTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private LiveReplayService liveReplayService;

  @InjectMocks
  private RunReplayImmediatelyMenuItem instance;

  @Test
  public void testOnClickedRunReplayImmediately() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    instance.setObject(game);
    instance.onClicked();
    verify(liveReplayService).performActionWhenAvailable(game, LiveReplayAction.RUN_REPLAY);
  }

  @Test
  public void testVisibleItemIfNoTrackingReplay() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(liveReplayService.getTrackingReplay()).thenReturn(Optional.empty());

    instance.setObject(game);
    assertTrue(instance.isVisible());
  }

  @Test
  public void testVisibleItemIfNoOwnTrackingReplay() {
    GameBean game = GameBeanBuilder.create().defaultValues().id(1).get();
    when(liveReplayService.getTrackingReplay()).thenReturn(Optional.of(new Pair<>(2, any())));

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
  public void testInvisibleItemIfTrackingOwnReplay() {
    GameBean game = GameBeanBuilder.create().defaultValues().id(1).get();
    when(liveReplayService.getTrackingReplay()).thenReturn(Optional.of(new Pair<>(1, LiveReplayAction.RUN_REPLAY)));

    instance.setObject(game);
    assertFalse(instance.isVisible());
  }
}