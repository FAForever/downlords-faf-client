package com.faforever.client.replay;

import com.faforever.client.api.dto.Validity;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.rating.RatingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.ReviewBuilder;
import com.faforever.client.vault.review.ReviewController;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import javafx.collections.FXCollections;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
  private NotificationService notificationService;
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

  private Player currentPlayer;
  private Replay onlineReplay;
  private Replay localReplay;

  @Before
  public void setUp() throws Exception {
    currentPlayer = PlayerBuilder.create("junit").defaultValues().get();
    onlineReplay = ReplayInfoBeanBuilder.create().defaultValues()
        .validity(Validity.VALID)
        .featuredMod(new FeaturedMod())
        .reviews(FXCollections.emptyObservableList())
        .title("test")
        .get();

    localReplay = ReplayInfoBeanBuilder.create().defaultValues()
        .validity(Validity.VALID)
        .featuredMod(new FeaturedMod())
        .reviews(FXCollections.emptyObservableList())
        .title("test")
        .replayFile(Paths.get("foo.tmp"))
        .get();

    instance = new ReplayDetailController(timeService, i18n, uiService, replayService, ratingService, mapService, playerService, clientProperties, notificationService, reviewService);

    when(reviewsController.getRoot()).thenReturn(new Pane());
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(new Player("junit")));
    when(replayService.getSize(onlineReplay.getId())).thenReturn(CompletableFuture.completedFuture(12));
    when(timeService.asDate(LocalDateTime.MIN)).thenReturn("Min Date");
    when(timeService.asShortTime(LocalDateTime.MIN)).thenReturn("Min Time");
    when(timeService.shortDuration(any(Duration.class))).thenReturn("Forever");
    when(i18n.get("game.onUnknownMap")).thenReturn("unknown map");
    when(i18n.get("unknown")).thenReturn("unknown");
    when(i18n.number(anyInt())).thenReturn("1234");
    when(i18n.get("game.idFormat", onlineReplay.getId())).thenReturn(String.valueOf(onlineReplay.getId()));

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

    assertFalse(instance.moreInformationPane.isVisible());
    assertFalse(instance.moreInformationPane.isManaged());
  }

  @Test
  public void setReplayOnline() {
    when(replayService.getSize(onlineReplay.getId())).thenReturn(CompletableFuture.completedFuture(1024));
    when(ratingService.calculateQuality(onlineReplay)).thenReturn(0.427);
    when(i18n.get(eq("percentage"), anyLong())).thenReturn("42");

    instance.setReplay(onlineReplay);

    assertTrue(instance.ratingSeparator.isVisible());
    assertTrue(instance.reviewSeparator.isVisible());
    assertTrue(instance.reviewsContainer.isVisible());
    assertTrue(instance.teamsInfoBox.isVisible());
    assertTrue(instance.downloadMoreInfoButton.isVisible());
    assertEquals(instance.onMapLabel.getText(), "unknown map");
    assertEquals(instance.dateLabel.getText(), "Min Date");
    assertEquals(instance.timeLabel.getText(), "Min Time");
    assertEquals(instance.durationLabel.getText(), "Forever");
    assertEquals(instance.replayIdField.getText(), String.valueOf(onlineReplay.getId()));
    assertEquals(instance.modLabel.getText(), "unknown");
    assertEquals(instance.titleLabel.getText(), "test");
    assertEquals(instance.playerCountLabel.getText(), "1234");
    assertEquals(instance.qualityLabel.getText(), "42");
  }

  @Test
  public void setReplayLocal() {
    when(ratingService.calculateQuality(localReplay)).thenReturn(Double.NaN);

    instance.setReplay(localReplay);
    verify(replayService).enrich(localReplay, localReplay.getReplayFile());
    assertEquals(instance.replayIdField.getText(), String.valueOf(localReplay.getId()));
    assertFalse(instance.ratingSeparator.isVisible());
    assertFalse(instance.reviewSeparator.isVisible());
    assertFalse(instance.reviewsContainer.isVisible());
    assertFalse(instance.teamsInfoBox.isVisible());
    assertFalse(instance.downloadMoreInfoButton.isVisible());
    assertFalse(instance.showRatingChangeButton.isVisible());
    assertTrue(instance.optionsTable.isVisible());
    assertTrue(instance.chatTable.isVisible());
    assertTrue(instance.moreInformationPane.isVisible());
    assertEquals(instance.titleLabel.textProperty().get(), "test");
  }

  @Test
  public void testReasonShownNotRated() {
    Replay replay = ReplayInfoBeanBuilder.create().defaultValues()
        .validity(Validity.HAS_AI)
        .get();

    when(replayService.getSize(replay.getId())).thenReturn(CompletableFuture.completedFuture(1024));
    when(ratingService.calculateQuality(replay)).thenReturn(0.427);
    when(i18n.get("game.reasonNotValid", i18n.get(replay.getValidity().getI18nKey()))).thenReturn("Reason: HAS_AI");

    instance.setReplay(replay);

    assertTrue(instance.notRatedReasonLabel.isVisible());
    assertEquals("Reason: HAS_AI", instance.notRatedReasonLabel.getText());
  }

  @Test
  public void tickTimeDisplayed() {
    when(replayService.getSize(anyInt())).thenReturn(CompletableFuture.completedFuture(1024));
    when(timeService.shortDuration(any())).thenReturn("16min 40s");
    Replay replay = ReplayInfoBeanBuilder.create().defaultValues().replayTicks(10_000).get();

    instance.setReplay(replay);

    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.replayDurationLabel.isVisible());
    assertTrue(instance.durationLabel.isVisible());

    assertEquals("16min 40s", instance.replayDurationLabel.getText());
  }

  @Test
  public void onDownloadMoreInfoClicked() {
    when(replayService.getSize(anyInt())).thenReturn(CompletableFuture.completedFuture(1024));
    Replay replay = ReplayInfoBeanBuilder.create().defaultValues().get();
    Review review = ReviewBuilder.create().defaultValues().player(new Player("junit")).get();
    replay.getReviews().add(review);

    instance.setReplay(replay);
    Path tmpPath = Paths.get("foo.tmp");
    when(replayService.downloadReplay(replay.getId())).thenReturn(CompletableFuture.completedFuture(tmpPath));

    instance.onDownloadMoreInfoClicked();

    WaitForAsyncUtils.waitForFxEvents();

    verify(replayService).enrich(replay, tmpPath);
    assertTrue(instance.optionsTable.isVisible());
    assertTrue(instance.chatTable.isVisible());
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.getRoot(), instance.replayDetailRoot);
    assertNull(instance.getRoot().getParent());
  }

  @Test
  public void onCloseButtonClicked() {
    instance.onCloseButtonClicked();

    assertFalse(instance.getRoot().isVisible());
  }

  @Test
  public void testOnDimmerClicked() {
    instance.onDimmerClicked();

    assertFalse(instance.getRoot().isVisible());
  }

  @Test
  public void testOnContentPaneClicked() {
    MouseEvent event = mock(MouseEvent.class);
    instance.onContentPaneClicked(event);

    verify(event).consume();
  }

  @Test
  public void testOnDeleteReview() {
    Review review = ReviewBuilder.create().defaultValues().player(currentPlayer).get();

    Replay replay = ReplayInfoBeanBuilder.create().defaultValues().get();
    replay.getReviews().add(review);

    instance.setReplay(replay);

    when(reviewService.deleteGameReview(review)).thenReturn(CompletableFuture.completedFuture(null));

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).deleteGameReview(review);
    assertFalse(replay.getReviews().contains(review));
  }

  @Test
  public void testOnDeleteReviewThrowsException() {
    Review review = ReviewBuilder.create().defaultValues().player(currentPlayer).get();

    Replay replay = ReplayInfoBeanBuilder.create().defaultValues().get();
    replay.getReviews().add(review);

    instance.setReplay(replay);

    when(reviewService.deleteGameReview(review)).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.delete.error"));
    assertTrue(replay.getReviews().contains(review));
  }

  @Test
  public void testOnSendReviewNew() {
    Review review = ReviewBuilder.create().defaultValues().id(null).get();

    Replay replay = ReplayInfoBeanBuilder.create().defaultValues().get();

    instance.setReplay(replay);

    when(reviewService.saveGameReview(review, replay.getId())).thenReturn(CompletableFuture.completedFuture(null));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveGameReview(review, replay.getId());
    assertTrue(replay.getReviews().contains(review));
    assertEquals(review.getPlayer(), currentPlayer);
  }

  @Test
  public void testOnSendReviewUpdate() {
    Review review = ReviewBuilder.create().defaultValues().get();

    Replay replay = ReplayInfoBeanBuilder.create().defaultValues().get();
    replay.getReviews().add(review);

    instance.setReplay(replay);

    when(reviewService.saveGameReview(review, replay.getId())).thenReturn(CompletableFuture.completedFuture(null));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveGameReview(review, replay.getId());
    assertTrue(replay.getReviews().contains(review));
    assertEquals(review.getPlayer(), currentPlayer);
    assertEquals(replay.getReviews().size(), 1);
  }

  @Test
  public void testOnSendReviewThrowsException() {
    Review review = ReviewBuilder.create().defaultValues().player(currentPlayer).get();

    Replay replay = ReplayInfoBeanBuilder.create().defaultValues().get();
    replay.getReviews().add(review);

    instance.setReplay(replay);

    when(reviewService.saveGameReview(review, replay.getId())).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.save.error"));
    assertTrue(replay.getReviews().contains(review));
  }

}
