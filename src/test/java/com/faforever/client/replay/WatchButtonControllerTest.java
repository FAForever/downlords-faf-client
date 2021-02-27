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

public class WatchButtonControllerTest extends AbstractPlainJavaFxTest {

  private static final int WATCH_DELAY = 10; // in seconds;

  @Mock
  ReplayService replayService;
  @Mock
  TimeService timeService;
  @Mock
  I18n i18n;

  WatchButtonController instance;

  @Before
  public void setUp() throws Exception {
    ClientProperties properties = new ClientProperties();
    properties.getReplay().setWatchDelaySeconds(WATCH_DELAY);

    instance = new WatchButtonController(replayService, properties, timeService, i18n);
    loadFxml("theme/vault/replay/watch_button.fxml",  clazz -> instance);
  }

  @Test
  public void testButtonWhenWatchNotAllowed() {
    Instant startTime = Instant.now().minus(5, ChronoUnit.SECONDS);
    Game game = GameBuilder.create().defaultValues().startTime(startTime).get();

    setGame(game);
    assertThat(instance.watchButton.isDisabled(), is(true));
  }

  @Test
  public void testButtonWhenWatchAllowed() {
    Instant startTime = Instant.now().minus(15, ChronoUnit.SECONDS);
    Game game = GameBuilder.create().defaultValues().startTime(startTime).get();

    setGame(game);
    assertThat(instance.watchButton.isDisabled(), is(false));
  }

  private void setGame(Game game) {
    runOnFxThreadAndWait(() -> instance.setGame(game));
  }

  @After
  public void stopTimer() {
    Timeline timeline = instance.getDelayTimeline();
    if (timeline != null) {
      timeline.stop();
    }
  }
}
