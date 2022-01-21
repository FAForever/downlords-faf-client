package com.faforever.client.replay;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.GameBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.replay.WatchButtonController;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WatchButtonControllerTest extends UITest {

  private static final int WATCH_DELAY = 10; // in seconds;

  @Mock
  private ReplayService replayService;
  @Mock
  private LiveReplayService liveReplayService;
  @Mock
  private I18n i18n;
  @Mock
  private TimeService timeService;
  @Spy
  private ClientProperties clientProperties = new ClientProperties();

  @InjectMocks
  private WatchButtonController instance;
  private GameBean game;

  @BeforeEach
  public void setUp() throws Exception {
    clientProperties.getReplay().setWatchDelaySeconds(WATCH_DELAY);
    when(liveReplayService.getTrackingReplayProperty()).thenReturn(new SimpleObjectProperty<>(null));

    game = GameBeanBuilder.create().defaultValues().get();
    loadFxml("theme/vault/replay/watch_button.fxml",  clazz -> instance);
  }

  @Test
  public void testButtonWhenWatchNotAllowed() {
    game.setStartTime(OffsetDateTime.now().minus(5, ChronoUnit.SECONDS));

    setGame(game);
    assertThat(instance.watchButton.getPseudoClassStates().contains(WatchButtonController.AVAILABLE_PSEUDO_CLASS), is(false));
  }

  @Test
  public void testButtonOnClickedWhenWatchNotAllowed() {
    game.setStartTime(OffsetDateTime.now().minus(5, ChronoUnit.SECONDS));

    setGame(game);
    clickWatchButton();
    verify(replayService, never()).runLiveReplay(game.getId());
  }

  @Test
  public void testButtonWhenWatchAllowed() {
    game.setStartTime(OffsetDateTime.now().minus(15, ChronoUnit.SECONDS));

    setGame(game);
    assertThat(instance.watchButton.isDisabled(), is(false));
  }

  @Test
  public void testButtonOnClickedWhenWatchAllowed() {
    when(liveReplayService.canWatch(game)).thenReturn(true);
    game.setStartTime(OffsetDateTime.now().minus(15, ChronoUnit.SECONDS));

    setGame(game);
    clickWatchButton();
    verify(replayService).runLiveReplay(game.getId());
  }

  private void setGame(GameBean game) {
    runOnFxThreadAndWait(() -> instance.setGame(game));
  }

  private void clickWatchButton() {
    runOnFxThreadAndWait(()-> instance.watchButton.fire());
  }

  @AfterEach
  public void stopTimer() {
    Timeline timeline = instance.getDelayTimeline();
    if (timeline != null) {
      timeline.stop();
    }
  }
}
