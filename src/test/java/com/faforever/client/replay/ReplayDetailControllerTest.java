package com.faforever.client.replay;

import com.faforever.client.builders.MapBeanBuilder;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PlayerStatsMapBuilder;
import com.faforever.client.builders.ReplayBeanBuilder;
import com.faforever.client.builders.ReplayReviewBeanBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayReviewBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.game.TeamCardController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.rating.RatingService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.ReviewController;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import com.faforever.commons.api.dto.Validity;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReplayDetailControllerTest extends PlatformTest {

  @InjectMocks
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
  private MapGeneratorService mapGeneratorService;
  @Mock
  private PlayerService playerService;
  @Mock
  private ReviewService reviewService;
  @Mock
  private ImageViewHelper imageViewHelper;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;
  @Mock
  private ReviewsController<ReplayReviewBean> reviewsController;
  @Mock
  private ReviewController<ReplayReviewBean> reviewController;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;
  @Mock
  private TeamCardController teamCardController;
  @Mock
  private ReportDialogController reportDialogController;
  private PlayerBean currentPlayer;
  private ReplayBean onlineReplay;
  private ReplayBean localReplay;
  private MapVersionBean mapBean;

  private final BooleanProperty installed = new SimpleBooleanProperty();

  @BeforeEach
  public void setUp() throws Exception {
    currentPlayer = PlayerBeanBuilder.create().defaultValues().get();
    mapBean = MapVersionBeanBuilder.create().defaultValues().map(MapBeanBuilder.create().defaultValues().get()).get();
    onlineReplay = ReplayBeanBuilder.create().defaultValues()
        .validity(Validity.VALID)
        .featuredMod(new FeaturedModBean())
        .title("test")
        .mapVersion(mapBean)
        .teamPlayerStats(PlayerStatsMapBuilder.create().defaultValues().get())
        .get();

    localReplay = ReplayBeanBuilder.create().defaultValues()
        .local(true)
        .validity(null)
        .featuredMod(new FeaturedModBean())
        .title("test")
        .replayFile(Path.of("foo.tmp"))
        .get();

    when(i18n.get("game.notRatedYet")).thenReturn("not rated yet");
    when(replayService.loadReplayDetails(any())).thenReturn(new ReplayDetails(List.of(), List.of(), mapBean));
    when(mapService.isInstalledBinding(Mockito.<MapVersionBean>any())).thenReturn(installed);
    when(imageViewHelper.createPlaceholderImageOnErrorObservable(any())).thenAnswer(invocation -> new SimpleObjectProperty<>(invocation.getArgument(0)));
    when(reviewService.getReplayReviews(any())).thenReturn(Flux.empty());
    when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());

    when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(currentPlayer));
    when(reviewsController.getRoot()).thenReturn(new Pane());
    when(mapService.loadPreview(anyString(), eq(PreviewSize.LARGE))).thenReturn(new Image(InputStream.nullInputStream()));
    when(playerService.getCurrentPlayer()).thenReturn(PlayerBeanBuilder.create().defaultValues().get());
    when(playerService.getPlayersByIds(any())).thenReturn(CompletableFuture.completedFuture(List.of(PlayerBeanBuilder.create()
        .defaultValues()
        .get())));
    when(replayService.getFileSize(onlineReplay)).thenReturn(CompletableFuture.completedFuture(12));
    when(replayService.replayChangedRating(onlineReplay)).thenReturn(true);
    when(timeService.asDate(onlineReplay.getStartTime())).thenReturn("Min Date");
    when(timeService.asShortTime(onlineReplay.getStartTime())).thenReturn("Min Time");
    when(timeService.asDate(localReplay.getStartTime())).thenReturn("Min Date");
    when(timeService.asShortTime(localReplay.getStartTime())).thenReturn("Min Time");
    when(timeService.shortDuration(any(Duration.class))).thenReturn("Forever");
    when(i18n.get("game.onUnknownMap")).thenReturn("unknown map");
    when(i18n.get("unknown")).thenReturn("unknown");
    when(i18n.number(anyInt())).thenReturn("1234");
    when(i18n.get("game.idFormat", onlineReplay.getId())).thenReturn(String.valueOf(onlineReplay.getId()));
    when(i18n.get("game.onMapFormat", mapBean.getMap().getDisplayName())).thenReturn(mapBean.getMap().getDisplayName());
    when(uiService.loadFxml("theme/team_card.fxml")).thenReturn(teamCardController);
    when(teamCardController.getRoot()).thenReturn(new HBox());
    when(uiService.loadFxml("theme/reporting/report_dialog.fxml")).thenReturn(reportDialogController);
    when(reportDialogController.getRoot()).thenReturn(new Pane());

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
  }

  @Test
  public void setReplayOnline() {
    when(replayService.getFileSize(onlineReplay)).thenReturn(CompletableFuture.completedFuture(1024));
    when(ratingService.calculateQuality(onlineReplay)).thenReturn(0.427);
    when(i18n.get(eq("percentage"), eq(Math.round(0.427 * 100)))).thenReturn("42");

    runOnFxThreadAndWait(() -> instance.setReplay(onlineReplay));

    verify(mapService).loadPreview(mapBean, PreviewSize.SMALL);
    assertTrue(instance.ratingSeparator.isVisible());
    assertTrue(instance.reviewSeparator.isVisible());
    assertTrue(instance.reviewsContainer.isVisible());
    assertTrue(instance.teamsInfoBox.isVisible());
    assertTrue(instance.downloadMoreInfoButton.isVisible());
    assertTrue(instance.showRatingChangeButton.isVisible());
    assertFalse(instance.showRatingChangeButton.isDisabled());
    assertFalse(instance.deleteButton.isVisible());
    assertFalse(instance.deleteButton.isManaged());
    assertNotEquals("-", instance.ratingLabel.getText());
    assertEquals("Min Date", instance.dateLabel.getText());
    assertEquals("Min Time", instance.timeLabel.getText());
    assertEquals("Forever", instance.durationLabel.getText());
    assertEquals(String.valueOf(onlineReplay.getId()), instance.replayIdField.getText());
    assertEquals("unknown", instance.modLabel.getText());
    assertEquals("test", instance.titleLabel.getText());
    assertEquals("1234", instance.playerCountLabel.getText());
    assertEquals("42", instance.qualityLabel.getText());
    assertEquals(mapBean.getMap().getDisplayName(), instance.onMapLabel.getText());
  }

  @Test
  public void setReplayLocal() throws Exception {
    when(replayService.loadReplayDetails(any())).thenReturn(new ReplayDetails(localReplay.getChatMessages(), localReplay.getGameOptions(), mapBean));
    when(ratingService.calculateQuality(localReplay)).thenReturn(Double.NaN);

    runOnFxThreadAndWait(() -> instance.setReplay(localReplay));

    verify(replayService).loadReplayDetails(localReplay.getReplayFile());
    assertEquals(String.valueOf(localReplay.getId()), instance.replayIdField.getText());
    assertFalse(instance.ratingSeparator.isVisible());
    assertFalse(instance.reviewSeparator.isVisible());
    assertFalse(instance.reviewsContainer.isVisible());
    assertFalse(instance.downloadMoreInfoButton.isVisible());
    assertFalse(instance.showRatingChangeButton.isVisible());
    assertTrue(instance.deleteButton.isVisible());
    assertTrue(instance.deleteButton.isManaged());
    assertTrue(instance.optionsTable.isVisible());
    assertTrue(instance.chatTable.isVisible());
    assertTrue(instance.moreInformationPane.isVisible());
    assertEquals("test", instance.titleLabel.textProperty().get());
  }

  @Test
  public void setReplayNoEndTime() {
    onlineReplay.setEndTime(null);

    runOnFxThreadAndWait(() -> instance.setReplay(onlineReplay));

    assertFalse(instance.durationLabel.isVisible());
  }

  @Test
  public void setReplayNoTeamStats() {
    onlineReplay.setTeamPlayerStats(FXCollections.emptyObservableMap());

    runOnFxThreadAndWait(() -> instance.setReplay(onlineReplay));
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("-", instance.ratingLabel.getText());
  }

  @Test
  public void setReplayMissing() {
    onlineReplay.setReplayAvailable(false);
    onlineReplay.setStartTime(OffsetDateTime.now().minusDays(2));

    when(i18n.get("game.replayFileMissing")).thenReturn("missing");

    runOnFxThreadAndWait(() -> instance.setReplay(onlineReplay));

    assertEquals("missing", instance.watchButton.getText());
    assertEquals("missing", instance.downloadMoreInfoButton.getText());
    assertTrue(instance.watchButton.isDisabled());
    assertFalse(instance.downloadMoreInfoButton.isVisible());
  }

  @Test
  public void setReplayNotAvailable() {
    onlineReplay.setReplayAvailable(false);
    onlineReplay.setReplayFile(null);

    when(i18n.get("game.replayFileMissing")).thenReturn("not available");

    runOnFxThreadAndWait(() -> instance.setReplay(onlineReplay));

    assertEquals("not available", instance.watchButton.getText());
    assertTrue(instance.watchButton.isDisabled());
    assertFalse(instance.downloadMoreInfoButton.isVisible());
  }

  @Test
  public void setReplayNoRatingChange() {
    onlineReplay.setValidity(null);
    when(replayService.replayChangedRating(onlineReplay)).thenReturn(false);

    runOnFxThreadAndWait(() -> instance.setReplay(onlineReplay));

    assertFalse(instance.showRatingChangeButton.isVisible());
    assertTrue(instance.notRatedReasonLabel.isVisible());
    assertEquals("not rated yet", instance.notRatedReasonLabel.getText());
  }

  @Test
  public void testReasonShownNotRated() {
    ReplayBean replay = ReplayBeanBuilder.create().defaultValues()
        .validity(Validity.HAS_AI)
        .teamPlayerStats(FXCollections.observableMap(PlayerStatsMapBuilder.create().defaultValues().get()))
        .get();

    when(replayService.getFileSize(replay)).thenReturn(CompletableFuture.completedFuture(1024));
    when(ratingService.calculateQuality(replay)).thenReturn(0.427);
    when(i18n.getOrDefault(replay.getValidity().toString(), "game.reasonNotValid", i18n.get(replay.getValidity()
        .getI18nKey()))).thenReturn("Reason: HAS_AI");

    runOnFxThreadAndWait(() -> instance.setReplay(replay));
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.notRatedReasonLabel.isVisible());
    assertEquals("Reason: HAS_AI", instance.notRatedReasonLabel.getText());
  }

  @Test
  public void tickTimeDisplayed() {
    when(replayService.getFileSize(any())).thenReturn(CompletableFuture.completedFuture(1024));
    when(timeService.shortDuration(any())).thenReturn("16min 40s");
    ReplayBean replay = ReplayBeanBuilder.create().defaultValues().replayTicks(10_000).get();

    runOnFxThreadAndWait(() -> instance.setReplay(replay));

    assertTrue(instance.replayDurationLabel.isVisible());
    assertTrue(instance.durationLabel.isVisible());

    assertEquals("16min 40s", instance.replayDurationLabel.getText());
  }

  @Test
  public void onDownloadMoreInfoClicked() throws Exception {
    when(replayService.getFileSize(any())).thenReturn(CompletableFuture.completedFuture(1024));
    ReplayBean replay = ReplayBeanBuilder.create().defaultValues().get();
    ReplayReviewBean review = ReplayReviewBeanBuilder.create()
        .defaultValues()
        .player(PlayerBeanBuilder.create().defaultValues().get())
        .get();
    review.setReplay(replay);

    runOnFxThreadAndWait(() -> instance.setReplay(replay));
    WaitForAsyncUtils.waitForFxEvents();

    Path tmpPath = Path.of("foo.tmp");
    when(replayService.downloadReplay(replay.getId())).thenReturn(CompletableFuture.completedFuture(tmpPath));

    runOnFxThreadAndWait(() -> instance.onDownloadMoreInfoClicked());

    verify(replayService).loadReplayDetails(tmpPath);
    assertTrue(instance.optionsTable.isVisible());
    assertTrue(instance.chatTable.isVisible());
    assertFalse(instance.downloadMoreInfoButton.isVisible());
  }

  @Test
  public void onDownloadMoreInfoClickedException() {
    when(replayService.downloadReplay(anyInt())).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    runOnFxThreadAndWait(() -> {
      instance.setReplay(onlineReplay);
      instance.onDownloadMoreInfoClicked();
    });

    assertTrue(instance.downloadMoreInfoButton.isVisible());
  }

  @Test
  public void testOnWatchButtonClicked() {
    runOnFxThreadAndWait(() -> instance.setReplay(onlineReplay));
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
  public void testOnDeleteReview() {
    ReplayReviewBean review = ReplayReviewBeanBuilder.create().defaultValues().player(currentPlayer).get();

    ReplayBean replay = ReplayBeanBuilder.create().defaultValues().get();
    review.setReplay(replay);

    runOnFxThreadAndWait(() -> instance.setReplay(replay));

    when(reviewService.deleteGameReview(review)).thenReturn(Mono.empty());

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).deleteGameReview(review);
  }

  @Test
  public void testOnDeleteReviewThrowsException() {
    ReplayReviewBean review = ReplayReviewBeanBuilder.create().defaultValues().player(currentPlayer).get();

    ReplayBean replay = ReplayBeanBuilder.create().defaultValues().get();
    review.setReplay(replay);

    runOnFxThreadAndWait(() -> instance.setReplay(replay));

    when(reviewService.deleteGameReview(review)).thenReturn(Mono.error(new FakeTestException()));

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.delete.error"));
  }

  @Test
  public void testOnSendReviewNew() {
    ReplayReviewBean review = ReplayReviewBeanBuilder.create().defaultValues().id(null).get();

    ReplayBean replay = ReplayBeanBuilder.create().defaultValues().get();
    review.setReplay(replay);

    runOnFxThreadAndWait(() -> instance.setReplay(replay));

    when(reviewService.saveReplayReview(review)).thenReturn(Mono.empty());

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveReplayReview(review);
    assertEquals(currentPlayer, review.getPlayer());
  }

  @Test
  public void testOnSendReviewUpdate() {
    ReplayReviewBean review = ReplayReviewBeanBuilder.create().defaultValues().id(0).get();

    ReplayBean replay = ReplayBeanBuilder.create().defaultValues().get();
    review.setReplay(replay);

    runOnFxThreadAndWait(() -> instance.setReplay(replay));

    when(reviewService.saveReplayReview(review)).thenReturn(Mono.empty());

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveReplayReview(review);
    assertEquals(currentPlayer, review.getPlayer());
  }

  @Test
  public void testOnSendReviewThrowsException() {
    ReplayReviewBean review = ReplayReviewBeanBuilder.create().defaultValues().player(currentPlayer).get();

    ReplayBean replay = ReplayBeanBuilder.create().defaultValues().get();
    review.setReplay(replay);

    runOnFxThreadAndWait(() -> instance.setReplay(replay));

    when(reviewService.saveReplayReview(review)).thenReturn(Mono.error(new FakeTestException()));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.save.error"));
  }

  @Test
  public void testReport() {
    runOnFxThreadAndWait(() -> instance.setReplay(onlineReplay));
    instance.onReport();

    verify(reportDialogController).setReplay(onlineReplay);
    verify(reportDialogController).show();
  }

}
