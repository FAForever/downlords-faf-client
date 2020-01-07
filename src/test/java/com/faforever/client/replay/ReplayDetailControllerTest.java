package com.faforever.client.replay;

import com.faforever.client.api.dto.Validity;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.MouseEvents;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.rating.RatingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.ReviewController;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import javafx.collections.FXCollections;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReplayDetailControllerTest extends AbstractPlainJavaFxTest {
  private ReplayDetailController instance;

  @Mock
  private TimeService timeService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private ReplayService replayService;
  @Mock
  private RatingService ratingService;
  @Mock
  private MapService mapService;
  @Mock
  private PlayerService playerService;
  @Mock
  private ReviewService reviewService;
  @Mock
  private ReviewsController reviewsController;
  @Mock
  private ReviewController reviewController;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;
  @Mock
  private ClientProperties clientProperties;

  @Before
  public void setUp() throws Exception {
    instance = new ReplayDetailController(timeService, i18n, uiService, replayService, ratingService, mapService, playerService, reviewService, clientProperties);

    when(reviewsController.getRoot()).thenReturn(new Pane());
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(new Player("junit")));

    loadFxml("theme/vault/replay/replay_detail.fxml", param -> {
      if (param == ReviewsController.class) {
        return reviewsController;
      }
      if (param == StarsController.class) {
        return starsController;
      }
      if (param == StarController.class) {
        return starController;
      }
      if (param == ReviewController.class) {
        return reviewController;
      }
      return instance;
    });

    assertThat(instance.moreInformationPane.isVisible(), is(false));
    assertThat(instance.moreInformationPane.isManaged(), is(false));
  }

  @Test
  public void setReplay() throws Exception {
    Replay replay = new Replay();
    replay.setValidity(Validity.VALID);
    replay.setFeaturedMod(new FeaturedMod());
    replay.getReviews().setAll(FXCollections.emptyObservableList());
    when(replayService.getSize(replay.getId())).thenReturn(CompletableFuture.completedFuture(1024));
    when(ratingService.calculateQuality(replay)).thenReturn(0.427);

    instance.setReplay(replay);

    assertEquals(instance.qualityLabel.textProperty().get(), (i18n.get("percentage", 0.43)));
  }

  @Test
  public void tickTimeDisplayed() throws Exception {
    when(replayService.getSize(anyInt())).thenReturn(CompletableFuture.completedFuture(1024));
    when(timeService.shortDuration(any())).thenReturn("16min 40s");
    Replay replay = new Replay();
    replay.setValidity(Validity.VALID);
    replay.setReplayTicks(10_000);

    instance.setReplay(replay);

    assertThat(instance.replayDurationLabel.isVisible(), is(true));
    assertThat(instance.durationLabel.isVisible(), is(false));

    assertEquals("16min 40s", instance.replayDurationLabel.getText());
  }

  @Test
  public void onDownloadMoreInfoClicked() throws Exception {
    when(replayService.getSize(anyInt())).thenReturn(CompletableFuture.completedFuture(1024));
    Replay replay = new Replay();
    Review review = new Review();
    review.setPlayer(new Player("junit"));
    replay.getReviews().setAll(FXCollections.observableArrayList(review));
    replay.setValidity(Validity.VALID);

    replay.setFeaturedMod(new FeaturedMod());
    instance.setReplay(replay);
    Path tmpPath = Paths.get("foo.tmp");
    when(replayService.downloadReplay(replay.getId())).thenReturn(CompletableFuture.completedFuture(tmpPath));

    instance.onDownloadMoreInfoClicked();

    verify(replayService).enrich(replay, tmpPath);
    assertThat(instance.optionsTable.isVisible(), is(true));
    assertThat(instance.chatTable.isVisible(), is(true));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.replayDetailRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void onCloseButtonClicked() throws Exception {
    new Pane(instance.getRoot());
    instance.onCloseButtonClicked();
  }

  @Test
  public void onDimmerClicked() throws Exception {
    new Pane(instance.getRoot());
    instance.onDimmerClicked();
  }

  @Test
  public void onContentPaneClicked() throws Exception {
    MouseEvent event = MouseEvents.generateClick(MouseButton.PRIMARY, 1);
    instance.onContentPaneClicked(event);
    assertThat(event.isConsumed(), is(true));
  }
}
