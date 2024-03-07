package com.faforever.client.replay;

import com.faforever.client.builders.GameInfoBuilder;
import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WatchButtonControllerTest extends PlatformTest {

  @Mock
  private UiService uiService;
  @Mock
  private TimeService timeService;
  @Mock
  private LiveReplayService liveReplayService;
  @Mock
  private ReplayRunner replayRunner;
  @Mock
  private I18n i18n;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;

  @InjectMocks
  private WatchButtonController instance;

  private final ObjectProperty<TrackingLiveReplay> trackingLiveReplayProperty = new SimpleObjectProperty<>(null);

  private GameInfo game;

  @BeforeEach
  public void setUp() throws Exception {
    game = GameInfoBuilder.create().defaultValues().get();

    when(liveReplayService.trackingLiveReplayProperty()).thenReturn(trackingLiveReplayProperty);
    when(i18n.get("game.watch")).thenReturn("watch");

    loadFxml("theme/vault/replay/watch_button.fxml", clazz -> instance);
  }

  @Test
  public void testRunReplayWhenAvailable() {
    when(liveReplayService.canWatchReplay(game)).thenReturn(true);
    when(liveReplayService.canWatchReplay(any(OffsetDateTime.class))).thenReturn(true);

    setGame(game);
    clickWatchButton();
    verify(replayRunner).runWithLiveReplay(game);
    assertEquals("watch", instance.watchButton.getText());
  }

  @Test
  public void testWatchButtonStateWhenReplayIsTracking() {
    trackingLiveReplayProperty.set(new TrackingLiveReplay(game.getId(), TrackingLiveReplayAction.NOTIFY_ME));
    runOnFxThreadAndWait(() -> setGame(game));
    assertTrue(instance.watchButton.getPseudoClassStates().contains(WatchButtonController.TRACKABLE_PSEUDO_CLASS));
  }

  private void setGame(GameInfo game) {
    runOnFxThreadAndWait(() -> instance.setGame(game));
  }

  private void clickWatchButton() {
    runOnFxThreadAndWait(() -> instance.watchButton.fire());
  }

  @AfterEach
  public void stopTimer() {
    Timeline timeline = instance.getWatchTimeTimeline();
    if (timeline != null) {
      timeline.stop();
    }
  }
}
