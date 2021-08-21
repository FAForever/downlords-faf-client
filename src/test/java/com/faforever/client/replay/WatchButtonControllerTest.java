package com.faforever.client.replay;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.GameBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.replay.WatchButtonController;
import javafx.animation.Timeline;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class WatchButtonControllerTest extends UITest {

  private static final int WATCH_DELAY = 10; // in seconds;

  @Mock
  private ReplayService replayService;
  @Mock
  private TimeService timeService;
  @Mock
  private I18n i18n;

  private WatchButtonController instance;
  private GameBean game;

  @BeforeEach
  public void setUp() throws Exception {
    ClientProperties properties = new ClientProperties();
    properties.getReplay().setWatchDelaySeconds(WATCH_DELAY);

    game = GameBeanBuilder.create().defaultValues().get();
    instance = new WatchButtonController(replayService, properties, timeService, i18n);
    loadFxml("theme/vault/replay/watch_button.fxml",  clazz -> instance);
  }

  @Test
  public void testButtonWhenWatchNotAllowed() {
    game.setStartTime(OffsetDateTime.now().minus(5, ChronoUnit.SECONDS));

    setGame(game);
    assertThat(instance.watchButton.isDisabled(), is(true));
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
