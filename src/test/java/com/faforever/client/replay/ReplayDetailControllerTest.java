package com.faforever.client.replay;

import com.faforever.client.api.dto.Validity;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.game.TeamCardController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean;
import com.faforever.client.map.MapBeanBuilder;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.rating.RatingService;
import com.faforever.client.replay.Replay.PlayerStats;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.review.ReviewBuilder;
import com.faforever.client.vault.review.ReviewController;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.image.Image;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
  private TeamCardController teamCardController;
  @Mock
  private ClientProperties clientProperties;
  @Mock
  private ReportDialogController reportDialogController;

  private Player currentPlayer;
  private Replay onlineReplay;
  private Replay localReplay;
  private MapBean mapBean;

  @Before
  public void setUp() throws Exception {
    currentPlayer = PlayerBuilder.create("junit").defaultValues().get();
    mapBean = MapBeanBuilder.create().defaultValues().get();
    onlineReplay = ReplayBuilder.create().defaultValues()
        .validity(Validity.VALID)
        .featuredMod(new FeaturedMod())
        .reviews(FXCollections.emptyObservableList())
        .title("test")
        .map(mapBean)
        .get();

    localReplay = ReplayBuilder.create().defaultValues()
        .validity(Validity.VALID)
        .featuredMod(new FeaturedMod())
        .reviews(FXCollections.emptyObservableList())
        .title("test")
        .replayFile(Paths.get("foo.tmp"))
        .get();

    instance = new ReplayDetailController(timeService, i18n, uiService, replayService, ratingService, mapService, playerService, clientProperties, notificationService, reviewService);

    when(reviewsController.getRoot()).thenReturn(new Pane());
    when(mapService.loadPreview(anyString(), eq(PreviewSize.LARGE))).thenReturn(mock(Image.class));
    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(new Player("junit")));
    when(playerService.getPlayersByIds(any())).thenReturn(CompletableFuture.completedFuture(List.of(PlayerBuilder.create("junit").defaultValues().get())));
    when(replayService.getSize(onlineReplay.getId())).thenReturn(CompletableFuture.completedFuture(12));
    when(replayService.replayChangedRating(onlineReplay)).thenReturn(true);
    when(timeService.asDate(LocalDateTime.MIN)).thenReturn("Min Date");
    when(timeService.asShortTime(LocalDateTime.MIN)).thenReturn("Min Time");
    when(timeService.shortDuration(any(Duration.class))).thenReturn("Forever");
    when(i18n.get("game.onUnknownMap")).thenReturn("unknown map");
    when(i18n.get("unknown")).thenReturn("unknown");
    when(i18n.number(anyInt())).thenReturn("1234");
    when(i18n.get("game.idFormat", onlineReplay.getId())).thenReturn(String.valueOf(onlineReplay.getId()));
    when(i18n.get("game.onMapFormat", mapBean.getDisplayName())).thenReturn(mapBean.getDisplayName());
    when(uiService.loadFxml("theme/team_card.fxml")).thenReturn(teamCardController);
    when(uiService.loadFxml("theme/reporting/report_dialog.fxml")).thenReturn(reportDialogController);

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
    when(i18n.get(eq("percentage"), eq(Math.round(0.427 * 100)))).thenReturn("42");

    instance.setReplay(onlineReplay);
    WaitForAsyncUtils.waitForFxEvents();

    verify(mapService).loadPreview(mapBean.getFolderName(), PreviewSize.LARGE);
    assertTrue(instance.ratingSeparator.isVisible());
    assertTrue(instance.reviewSeparator.isVisible());
    assertTrue(instance.reviewsContainer.isVisible());
    assertTrue(instance.teamsInfoBox.isVisible());
    assertTrue(instance.downloadMoreInfoButton.isVisible());
    assertTrue(instance.showRatingChangeButton.isVisible());
    assertFalse(instance.showRatingChangeButton.isDisabled());
    assertNotEquals("-", instance.ratingLabel.getText());
    assertEquals("Min Date", instance.dateLabel.getText());
    assertEquals("Min Time", instance.timeLabel.getText());
    assertEquals("Forever", instance.durationLabel.getText());
    assertEquals(String.valueOf(onlineReplay.getId()), instance.replayIdField.getText());
    assertEquals("unknown", instance.modLabel.getText());
    assertEquals("test", instance.titleLabel.getText());
    assertEquals("1234", instance.playerCountLabel.getText());
    assertEquals("42", instance.qualityLabel.getText());
    assertEquals(mapBean.getDisplayName(), instance.onMapLabel.getText());
  }

  @Test
  public void setReplayLocal() {
    when(ratingService.calculateQuality(localReplay)).thenReturn(Double.NaN);

    instance.setReplay(localReplay);
    WaitForAsyncUtils.waitForFxEvents();

    verify(replayService).enrich(localReplay, localReplay.getReplayFile());
    assertEquals(String.valueOf(localReplay.getId()), instance.replayIdField.getText());
    assertFalse(instance.ratingSeparator.isVisible());
    assertFalse(instance.reviewSeparator.isVisible());
    assertFalse(instance.reviewsContainer.isVisible());
    assertFalse(instance.teamsInfoBox.isVisible());
    assertFalse(instance.downloadMoreInfoButton.isVisible());
    assertFalse(instance.showRatingChangeButton.isVisible());
    assertTrue(instance.optionsTable.isVisible());
    assertTrue(instance.chatTable.isVisible());
    assertTrue(instance.moreInformationPane.isVisible());
    assertEquals("test", instance.titleLabel.textProperty().get());
  }

  @Test
  public void setReplayNoEndTime() {
    onlineReplay.setEndTime(null);

    instance.setReplay(onlineReplay);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.durationLabel.isVisible());
  }

  @Test
  public void setReplayNoTeamStats() {
    onlineReplay.setTeamPlayerStats(FXCollections.emptyObservableMap());

    instance.setReplay(onlineReplay);
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("-", instance.ratingLabel.getText());
  }

  @Test
  public void setReplayNoOnlineFile() {
    when(replayService.getSize(onlineReplay.getId())).thenReturn(CompletableFuture.completedFuture(-1));
    when(i18n.get("game.replayFileMissing")).thenReturn("file missing");

    instance.setReplay(onlineReplay);
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("file missing", instance.watchButton.getText());
    assertEquals("file missing", instance.downloadMoreInfoButton.getText());
    assertTrue(instance.watchButton.isDisabled());
    assertTrue(instance.downloadMoreInfoButton.isDisabled());
  }

  @Test
  public void setReplayNotAvailable() {
    onlineReplay.setReplayAvailable(false);

    when(replayService.getSize(onlineReplay.getId())).thenReturn(CompletableFuture.completedFuture(-1));
    when(i18n.get("game.replayNotAvailable")).thenReturn("not available");

    instance.setReplay(onlineReplay);
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("not available", instance.watchButton.getText());
    assertEquals("not available", instance.downloadMoreInfoButton.getText());
    assertTrue(instance.watchButton.isDisabled());
    assertTrue(instance.downloadMoreInfoButton.isDisabled());
  }

  @Test
  public void setReplayNoRatingChange() {
    when(replayService.replayChangedRating(onlineReplay)).thenReturn(false);
    when(i18n.get("game.notRatedYet")).thenReturn("not rated yet");

    instance.setReplay(onlineReplay);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.showRatingChangeButton.isVisible());
    assertTrue(instance.notRatedReasonLabel.isVisible());
    assertEquals("not rated yet", instance.notRatedReasonLabel.getText());
  }

  @Test
  public void testReasonShownNotRated() {
    Replay replay = ReplayBuilder.create().defaultValues()
        .validity(Validity.HAS_AI)
        .teamPlayerStats(FXCollections.observableMap(PlayerStatsMapBuilder.create().defaultValues().get()))
        .get();

    when(replayService.getSize(replay.getId())).thenReturn(CompletableFuture.completedFuture(1024));
    when(ratingService.calculateQuality(replay)).thenReturn(0.427);
    when(i18n.getWithDefault(replay.getValidity().toString(), "game.reasonNotValid", i18n.get(replay.getValidity().getI18nKey()))).thenReturn("Reason: HAS_AI");

    instance.setReplay(replay);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.notRatedReasonLabel.isVisible());
    assertEquals("Reason: HAS_AI", instance.notRatedReasonLabel.getText());
  }

  @Test
  public void tickTimeDisplayed() {
    when(replayService.getSize(anyInt())).thenReturn(CompletableFuture.completedFuture(1024));
    when(timeService.shortDuration(any())).thenReturn("16min 40s");
    Replay replay = ReplayBuilder.create().defaultValues().replayTicks(10_000).get();

    instance.setReplay(replay);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.replayDurationLabel.isVisible());
    assertTrue(instance.durationLabel.isVisible());

    assertEquals("16min 40s", instance.replayDurationLabel.getText());
  }

  @Test
  public void onDownloadMoreInfoClicked() {
    when(replayService.getSize(anyInt())).thenReturn(CompletableFuture.completedFuture(1024));
    Replay replay = ReplayBuilder.create().defaultValues().get();
    Review review = ReviewBuilder.create().defaultValues().player(new Player("junit")).get();
    replay.getReviews().add(review);

    instance.setReplay(replay);
    WaitForAsyncUtils.waitForFxEvents();

    Path tmpPath = Paths.get("foo.tmp");
    when(replayService.downloadReplay(replay.getId())).thenReturn(CompletableFuture.completedFuture(tmpPath));

    instance.onDownloadMoreInfoClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(replayService).enrich(replay, tmpPath);
    assertTrue(instance.optionsTable.isVisible());
    assertTrue(instance.chatTable.isVisible());
    assertFalse(instance.downloadMoreInfoButton.isVisible());
  }

  @Test
  public void onDownloadMoreInfoClickedException() {
    when(replayService.downloadReplay(anyInt())).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    instance.setReplay(onlineReplay);
    WaitForAsyncUtils.waitForFxEvents();

    instance.onDownloadMoreInfoClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("replay.enrich.error"));
    assertFalse(instance.downloadMoreInfoButton.isVisible());
  }

  @Test
  public void testGetPlayerFaction() {
    Map<Integer, PlayerStats> statsByPlayerId = onlineReplay.getTeamPlayerStats().values().stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(PlayerStats::getPlayerId, Function.identity()));
    int id = statsByPlayerId.keySet().stream().findFirst().orElseThrow();
    PlayerStats playerStats = statsByPlayerId.get(id);
    Player player = PlayerBuilder.create("junit").defaultValues().id(id).get();
    assertEquals(playerStats.getFaction(), instance.getPlayerFaction(player, statsByPlayerId));
  }

  @Test
  public void testGetPlayerRating() {
    Map<Integer, PlayerStats> statsByPlayerId = onlineReplay.getTeamPlayerStats().values().stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toMap(PlayerStats::getPlayerId, Function.identity()));
    int id = statsByPlayerId.keySet().stream().findFirst().orElseThrow();
    PlayerStats playerStats = statsByPlayerId.get(id);
    Player player = PlayerBuilder.create("junit").defaultValues().id(id).get();
    assertEquals(Integer.valueOf(RatingUtil.getRating(playerStats.getBeforeMean(), playerStats.getBeforeDeviation())), instance.getPlayerRating(player, statsByPlayerId));
  }

  @Test
  public void testGetRoot() {
    Node root = instance.getRoot();
    assertEquals(instance.replayDetailRoot, root);
    assertNull(root.getParent());
  }

  @Test
  public void testOnWatchButtonClicked() {
    instance.setReplay(onlineReplay);
    instance.onWatchButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(replayService).runReplay(onlineReplay);
  }

  @Test
  public void testOnCloseButtonClicked() {
    instance.onCloseButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.getRoot().isVisible());
  }

  @Test
  public void testOnDimmerClicked() {
    instance.onDimmerClicked();
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.getRoot().isVisible());
  }

  @Test
  public void testOnContentPaneClicked() {
    MouseEvent event = mock(MouseEvent.class);
    instance.onContentPaneClicked(event);
    WaitForAsyncUtils.waitForFxEvents();

    verify(event).consume();
  }

  @Test
  public void testShowRatingChange() {
    instance.showRatingChange();
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.showRatingChangeButton.isDisabled());
  }

  @Test
  public void testOnDeleteReview() {
    Review review = ReviewBuilder.create().defaultValues().player(currentPlayer).get();

    Replay replay = ReplayBuilder.create().defaultValues().get();
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

    Replay replay = ReplayBuilder.create().defaultValues().get();
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

    Replay replay = ReplayBuilder.create().defaultValues().get();

    instance.setReplay(replay);

    when(reviewService.saveGameReview(review, replay.getId())).thenReturn(CompletableFuture.completedFuture(null));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveGameReview(review, replay.getId());
    assertTrue(replay.getReviews().contains(review));
    assertEquals(currentPlayer, review.getPlayer());
  }

  @Test
  public void testOnSendReviewUpdate() {
    Review review = ReviewBuilder.create().defaultValues().get();

    Replay replay = ReplayBuilder.create().defaultValues().get();
    replay.getReviews().add(review);

    instance.setReplay(replay);

    when(reviewService.saveGameReview(review, replay.getId())).thenReturn(CompletableFuture.completedFuture(null));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveGameReview(review, replay.getId());
    assertTrue(replay.getReviews().contains(review));
    assertEquals(currentPlayer, review.getPlayer());
    assertEquals(1, replay.getReviews().size());
  }

  @Test
  public void testOnSendReviewThrowsException() {
    Review review = ReviewBuilder.create().defaultValues().player(currentPlayer).get();

    Replay replay = ReplayBuilder.create().defaultValues().get();
    replay.getReviews().add(review);

    instance.setReplay(replay);

    when(reviewService.saveGameReview(review, replay.getId())).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.save.error"));
    assertTrue(replay.getReviews().contains(review));
  }

  @Test
  public void testReport() {
    instance.setReplay(onlineReplay);
    instance.onReport();

    verify(reportDialogController).setGame(onlineReplay);
    verify(reportDialogController).show();
  }

}
