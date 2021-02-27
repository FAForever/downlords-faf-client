package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.replay.WatchButtonController;
import javafx.animation.Timeline;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class WatchButtonControllerTest extends AbstractPlainJavaFxTest {

  private static final int WATCH_DELAY = 10; // in seconds;

  @Mock
  private ReplayService replayService;
  @Mock
  private TimeService timeService;
  @Mock
  private I18n i18n;

  private WatchButtonController instance;
  private Game game;

  @Before
  public void setUp() throws Exception {
    ClientProperties properties = new ClientProperties();
    properties.getReplay().setWatchDelaySeconds(WATCH_DELAY);

    game = GameBuilder.create().defaultValues().get();
    instance = new WatchButtonController(replayService, properties, timeService, i18n);
    loadFxml("theme/vault/replay/watch_button.fxml",  clazz -> instance);
  }

  @Test
  public void testButtonWhenWatchNotAllowed() {
    game.setStartTime(Instant.now().minus(5, ChronoUnit.SECONDS));

    setGame(game);
    assertThat(instance.watchButton.isDisabled(), is(true));
  }

  @Test
  public void testButtonOnClickedWhenWatchNotAllowed() {
    game.setStartTime(Instant.now().minus(5, ChronoUnit.SECONDS));

    setGame(game);
    clickWatchButton();
    verify(replayService, never()).runLiveReplay(game.getId());
  }

  @Test
  public void testButtonWhenWatchAllowed() {
    game.setStartTime(Instant.now().minus(15, ChronoUnit.SECONDS));

    setGame(game);
    assertThat(instance.watchButton.isDisabled(), is(false));
  }

  @Test
  public void testButtonOnClickedWhenWatchAllowed() {
    game.setStartTime(Instant.now().minus(15, ChronoUnit.SECONDS));

    setGame(game);
    clickWatchButton();
    verify(replayService).runLiveReplay(game.getId());
  }

  private void setGame(Game game) {
    runOnFxThreadAndWait(() -> instance.setGame(game));
  }

  private void clickWatchButton() {
    runOnFxThreadAndWait(()-> instance.watchButton.fire());
  }

  @After
  public void stopTimer() {
    Timeline timeline = instance.getDelayTimeline();
    if (timeline != null) {
      timeline.stop();
    }
  }
}
