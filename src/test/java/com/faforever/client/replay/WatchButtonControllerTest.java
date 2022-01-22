package com.faforever.client.replay;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.replay.LiveReplayService.LiveReplayAction;
import com.faforever.client.test.UITest;
import com.faforever.client.vault.replay.WatchButtonController;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WatchButtonControllerTest extends UITest {

  @Mock
  private ReplayService replayService;
  @Mock
  private LiveReplayService liveReplayService;
  @Mock
  private I18n i18n;

  @InjectMocks
  private WatchButtonController instance;
  private GameBean game;
  private final ObjectProperty<Pair<Integer, LiveReplayAction>> trackingReplayProperty = new SimpleObjectProperty<>(null);

  @BeforeEach
  public void setUp() throws Exception {
    when(liveReplayService.getTrackingReplayProperty()).thenReturn(trackingReplayProperty);
    when(i18n.get("vault.liveReplays.contextMenu.notifyMe")).thenReturn("notify me");
    when(i18n.get("vault.liveReplays.contextMenu.notifyMe.cancel")).thenReturn("cancel: notify me");
    when(i18n.get("vault.liveReplays.contextMenu.runImmediately")).thenReturn("run replay");
    when(i18n.get("vault.liveReplays.contextMenu.runImmediately.cancel")).thenReturn("cancel: run replay");

    game = GameBeanBuilder.create().defaultValues().get();
    loadFxml("theme/vault/replay/watch_button.fxml", clazz -> instance);
  }

  @Test
  public void testButtonWhenWatchNotAllowed() {
    when(liveReplayService.canWatchReplay(game)).thenReturn(false);

    setGame(game);
    assertFalse(instance.watchButton.getPseudoClassStates().contains(WatchButtonController.AVAILABLE_PSEUDO_CLASS));
    assertNull(instance.watchButton.getOnAction());
  }

  @Test
  public void testButtonOnClickedWhenWatchNotAllowed() {
    setGame(game);
    clickWatchButton();
    verify(replayService, never()).runLiveReplay(game.getId());
  }

  @Test
  public void testButtonWhenWatchAllowed() {
    when(liveReplayService.canWatchReplay(game)).thenReturn(true);

    setGame(game);
    assertTrue(instance.watchButton.getPseudoClassStates().contains(WatchButtonController.AVAILABLE_PSEUDO_CLASS));
  }

  @Test
  public void testButtonOnClickedWhenWatchAllowed() {
    when(liveReplayService.canWatchReplay(game)).thenReturn(true);

    setGame(game);
    clickWatchButton();
    verify(replayService).runLiveReplay(game.getId());
  }

  @Test
  public void testOnClickedNotifyMeWhenReplayIsAvailable() {
    when(liveReplayService.canWatchReplay(game)).thenReturn(false);

    setGame(game);
    runOnFxThreadAndWait(() -> instance.notifyMeItem.fire());
    verify(liveReplayService).performActionWhenAvailable(game, LiveReplayAction.NOTIFY_ME);
  }

  @Test
  public void testOnClickedNotifyMeWhenReplayIsAlreadyTracking() {
    when(liveReplayService.canWatchReplay(game)).thenReturn(false);

    setGame(game);
    runOnFxThreadAndWait(() -> {
      trackingReplayProperty.set(new Pair<>(1, LiveReplayAction.NOTIFY_ME));
      instance.notifyMeItem.fire();
    });
    verify(liveReplayService).stopTrackingReplay();
  }

  @Test
  public void testOnClickedRunReplayWhenReplayIsAvailable() {
    when(liveReplayService.canWatchReplay(game)).thenReturn(false);

    setGame(game);
    runOnFxThreadAndWait(() -> instance.runReplayItem.fire());
    verify(liveReplayService).performActionWhenAvailable(game, LiveReplayAction.RUN);
  }

  @Test
  public void testOnClickedRunReplayWhenReplayIsAlreadyTracking() {
    when(liveReplayService.canWatchReplay(game)).thenReturn(false);

    setGame(game);
    runOnFxThreadAndWait(() -> {
      trackingReplayProperty.set(new Pair<>(1, LiveReplayAction.RUN));
      instance.runReplayItem.fire();
    });
    verify(liveReplayService).stopTrackingReplay();
  }

  @Test
  public void testCheckNotifyMeItemTextWhenTrackingReplayPropertyIsChanged() {
    game.setId(1);
    when(liveReplayService.canWatchReplay(game)).thenReturn(false);

    setGame(game);
    assertEquals("notify me", instance.notifyMeItem.getText());
    runOnFxThreadAndWait(() -> trackingReplayProperty.set(new Pair<>(1, LiveReplayAction.NOTIFY_ME)));
    assertEquals("cancel: notify me", instance.notifyMeItem.getText());
  }

  @Test
  public void testCheckRunReplayItemTextWhenTrackingReplayPropertyIsChanged() {
    game.setId(1);
    when(liveReplayService.canWatchReplay(game)).thenReturn(false);

    setGame(game);
    assertEquals("run replay", instance.runReplayItem.getText());
    runOnFxThreadAndWait(() -> trackingReplayProperty.set(new Pair<>(1, LiveReplayAction.RUN)));
    assertEquals("cancel: run replay", instance.runReplayItem.getText());
  }

  private void setGame(GameBean game) {
    runOnFxThreadAndWait(() -> instance.setGame(game));
  }

  private void clickWatchButton() {
    runOnFxThreadAndWait(() -> instance.watchButton.fire());
  }

  @AfterEach
  public void stopTimer() {
    Timeline timeline = instance.getDelayTimeline();
    if (timeline != null) {
      timeline.stop();
    }
  }
}
