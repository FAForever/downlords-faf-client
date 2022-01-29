package com.faforever.client.replay;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder.MenuItemBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.replay.LiveReplayService.LiveReplayAction;
import com.faforever.client.test.UITest;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.replay.WatchButtonController;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.util.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class WatchButtonControllerTest extends UITest {

  @Mock
  private TimeService timeService;
  @Mock
  private LiveReplayService liveReplayService;
  @Mock
  private I18n i18n;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;

  @InjectMocks
  private WatchButtonController instance;

  private GameBean game;
  private ObjectProperty<Pair<Integer, LiveReplayAction>> trackingReplayProperty;

  @BeforeEach
  public void setUp() throws Exception {
    game = GameBeanBuilder.create().defaultValues().get();
    trackingReplayProperty = new SimpleObjectProperty<>(null);

    when(liveReplayService.getTrackingReplayProperty()).thenReturn(trackingReplayProperty);
    when(liveReplayService.getTrackingReplay()).thenReturn(Optional.ofNullable(trackingReplayProperty.get()));
    when(i18n.get("game.watch")).thenReturn("watch");

    loadFxml("theme/vault/replay/watch_button.fxml", clazz -> instance);
  }

  @Test
  public void testShowContextMenuIfReplayUnavailableYet() {
    ContextMenu contextMenu = mockContextMenuBuilderAndGetContextMenuMock();
    setGame(game);
    clickWatchButton();
    verify(contextMenu).show(instance.watchButton, Side.BOTTOM, 0 ,0);
    verifyNoInteractions(liveReplayService);
  }

  @Test
  public void testRunReplayWhenAvailable() {
    when(liveReplayService.canWatchReplay(game)).thenReturn(true);

    setGame(game);
    clickWatchButton();
    verify(liveReplayService).runLiveReplay(game.getId());
    assertEquals("watch", instance.watchButton.getText());
  }

  @Test
  public void testWatchButtonStateWhenReplayIsTracking() {
    when(liveReplayService.getTrackingReplay()).thenReturn(Optional.of(new Pair<>(game.getId(), LiveReplayAction.NOTIFY_ME)));
    setGame(game);
    assertTrue(instance.watchButton.getPseudoClassStates().contains(WatchButtonController.TRACKABLE_PSEUDO_CLASS));
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

  private ContextMenu mockContextMenuBuilderAndGetContextMenuMock() {
    MenuItemBuilder menuItemBuilder = mock(MenuItemBuilder.class, Answers.RETURNS_SELF);
    when(contextMenuBuilder.newBuilder()).thenReturn(menuItemBuilder);
    ContextMenu contextMenu = mock(ContextMenu.class);
    when(menuItemBuilder.build()).thenReturn(contextMenu);
    return contextMenu;
  }
}
