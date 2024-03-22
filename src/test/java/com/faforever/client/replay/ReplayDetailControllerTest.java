package com.faforever.client.replay;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.api.GamePlayerStats;
import com.faforever.client.domain.api.LeagueScoreJournal;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.Replay;
import com.faforever.client.domain.api.ReplayReview;
import com.faforever.client.domain.server.PlayerInfo;
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
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.instancio.Instancio;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
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
  private ReviewsController<ReplayReview> reviewsController;
  @Mock
  private ReviewController<ReplayReview> reviewController;
  @Mock
  private StarsController starsController;
  @Mock
  private StarController starController;
  @Mock
  private TeamCardController teamCardController;
  @Mock
  private ReportDialogController reportDialogController;
  private PlayerInfo currentPlayer;
  private Replay onlineReplay;
  private Replay localReplay;
  private MapVersion mapBean;

  private final BooleanProperty installed = new SimpleBooleanProperty();

  @BeforeEach
  public void setUp() throws Exception {
    currentPlayer = PlayerInfoBuilder.create().defaultValues().get();
    mapBean = Instancio.create(MapVersion.class);
    Map<String, List<GamePlayerStats>> teamPlayerStats = new HashMap<>();
    teamPlayerStats.put("2", List.of(Instancio.of(GamePlayerStats.class).create()));
    onlineReplay = Instancio.of(Replay.class)
                            .set(field(Replay::validity), Validity.VALID)
                            .set(field(Replay::title), "test")
                            .set(field(Replay::mapVersion), mapBean)
                            .set(field(Replay::replayAvailable), true)
                            .set(field(Replay::teamPlayerStats), teamPlayerStats)
                            .ignore(field(Replay::replayFile))
                            .ignore(field(Replay::local))
                            .create();

    localReplay = Instancio.of(Replay.class)
                           .set(field(Replay::local), true)
                           .set(field(Replay::title), "test")
                           .set(field(Replay::replayFile), Path.of("foo.tmp"))
                           .ignore(field(Replay::validity))
                           .create();

    lenient().when(i18n.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(replayService.loadReplayDetails(any())).thenReturn(new ReplayDetails(List.of(), List.of(), mapBean));
    lenient().when(mapService.isInstalledBinding(Mockito.<MapVersion>any())).thenReturn(installed);
    lenient().when(imageViewHelper.createPlaceholderImageOnErrorObservable(any()))
             .thenAnswer(invocation -> new SimpleObjectProperty<>(invocation.getArgument(0)));
    lenient().when(reviewService.getReplayReviews(any())).thenReturn(Flux.empty());
    lenient().when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());

    lenient().when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(currentPlayer));
    lenient().when(reviewsController.getRoot()).thenReturn(new Pane());
    lenient().when(mapService.loadPreview(anyString(), eq(PreviewSize.LARGE)))
             .thenReturn(new Image(InputStream.nullInputStream()));
    lenient().when(playerService.getCurrentPlayer()).thenReturn(PlayerInfoBuilder.create().defaultValues().get());
    lenient().when(playerService.getPlayersByIds(any()))
             .thenReturn(Flux.just(PlayerInfoBuilder.create().defaultValues().get()));
    lenient().when(replayService.getFileSize(onlineReplay)).thenReturn(CompletableFuture.completedFuture(12));
    lenient().when(replayService.replayChangedRating(onlineReplay)).thenReturn(true);
    lenient().when(replayService.getLeagueScoreJournalForReplay(any())).thenReturn(
        Flux.just(Instancio.create(LeagueScoreJournal.class)));
    lenient().when(timeService.asDate(onlineReplay.startTime())).thenReturn("Min Date");
    lenient().when(timeService.asShortTime(onlineReplay.startTime())).thenReturn("Min Time");
    lenient().when(timeService.asDate(localReplay.startTime())).thenReturn("Min Date");
    lenient().when(timeService.asShortTime(localReplay.startTime())).thenReturn("Min Time");
    lenient().when(timeService.shortDuration(any(Duration.class))).thenReturn("Forever");
    lenient().when(i18n.get("game.onUnknownMap")).thenReturn("unknown map");
    lenient().when(i18n.get("unknown")).thenReturn("unknown");
    lenient().when(i18n.number(anyInt())).thenReturn("1234");
    lenient().when(i18n.get(eq("game.idFormat"), any())).thenAnswer(invocation -> invocation.getArgument(1).toString());
    lenient().when(i18n.get(eq("game.onMapFormat"), any())).thenAnswer(invocation -> invocation.getArgument(1));
    lenient().when(uiService.loadFxml("theme/team_card.fxml")).thenReturn(teamCardController);
    lenient().when(teamCardController.getRoot()).thenReturn(new HBox());
    lenient().when(uiService.loadFxml("theme/reporting/report_dialog.fxml")).thenReturn(reportDialogController);
    lenient().when(reportDialogController.getRoot()).thenReturn(new Pane());

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
    when(ratingService.calculateQuality(onlineReplay)).thenReturn(0.427);
    when(i18n.get(eq("percentage"), eq(Math.round(0.427 * 100)))).thenReturn("42");

    runOnFxThreadAndWait(() -> instance.setReplay(onlineReplay));

    verify(mapService, atLeastOnce()).loadPreview(mapBean, PreviewSize.SMALL);
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
    assertEquals(String.valueOf(onlineReplay.id()), instance.replayIdField.getText());
    assertEquals(onlineReplay.featuredMod().displayName(), instance.modLabel.getText());
    assertEquals("test", instance.titleLabel.getText());
    assertEquals("1234", instance.playerCountLabel.getText());
    assertEquals("42", instance.qualityLabel.getText());
    assertEquals(mapBean.map().displayName(), instance.onMapLabel.getText());
  }

  @Test
  public void setReplayLocal() throws Exception {
    when(replayService.loadReplayDetails(any())).thenReturn(
        new ReplayDetails(localReplay.chatMessages(), localReplay.gameOptions(), mapBean));

    runOnFxThreadAndWait(() -> instance.setReplay(localReplay));

    verify(replayService, atLeastOnce()).loadReplayDetails(localReplay.replayFile());
    assertEquals(String.valueOf(localReplay.id()), instance.replayIdField.getText());
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
    Replay onlineReplay = Instancio.of(Replay.class).ignore(field(Replay::endTime)).create();

    runOnFxThreadAndWait(() -> instance.setReplay(onlineReplay));

    assertFalse(instance.durationLabel.isVisible());
  }

  @Test
  public void setReplayNoTeamStats() {
    Replay onlineReplay = Instancio.of(Replay.class).ignore(field(Replay::teamPlayerStats)).create();

    runOnFxThreadAndWait(() -> instance.setReplay(onlineReplay));
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("-", instance.ratingLabel.getText());
  }

  @Test
  public void setReplayMissing() {
    Replay onlineReplay = Instancio.of(Replay.class)
                                   .ignore(field(Replay::replayFile))
                                   .ignore(field(Replay::replayAvailable))
                                   .create();


    when(i18n.get("game.replayFileMissing")).thenReturn("missing");

    runOnFxThreadAndWait(() -> instance.setReplay(onlineReplay));

    assertEquals("missing", instance.watchButton.getText());
    assertEquals("missing", instance.downloadMoreInfoButton.getText());
    assertTrue(instance.watchButton.isDisabled());
    assertFalse(instance.downloadMoreInfoButton.isVisible());
  }

  @Test
  public void setReplayNotAvailable() {
    Replay onlineReplay = Instancio.of(Replay.class)
                                   .ignore(field(Replay::replayFile))
                                   .set(field(Replay::replayAvailable), false)
                                   .create();

    when(i18n.get("game.replayFileMissing")).thenReturn("not available");

    runOnFxThreadAndWait(() -> instance.setReplay(onlineReplay));

    assertEquals("not available", instance.watchButton.getText());
    assertTrue(instance.watchButton.isDisabled());
    assertFalse(instance.downloadMoreInfoButton.isVisible());
  }

  @Test
  public void setReplayNoRatingChange() {
    Replay onlineReplay = Instancio.of(Replay.class).ignore(field(Replay::validity)).create();

    runOnFxThreadAndWait(() -> instance.setReplay(onlineReplay));

    assertFalse(instance.showRatingChangeButton.isVisible());
    assertTrue(instance.notRatedReasonLabel.isVisible());
    assertEquals("game.notRatedYet", instance.notRatedReasonLabel.getText());
  }

  @Test
  public void testReasonShownNotRated() {
    Replay replay = Instancio.of(Replay.class).set(field(Replay::validity), Validity.HAS_AI).create();

    when(i18n.getOrDefault(replay.validity().toString(), "game.reasonNotValid", i18n.get(replay.validity()
        .getI18nKey()))).thenReturn("Reason: HAS_AI");

    runOnFxThreadAndWait(() -> instance.setReplay(replay));
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.notRatedReasonLabel.isVisible());
    assertEquals("Reason: HAS_AI", instance.notRatedReasonLabel.getText());
  }

  @Test
  public void tickTimeDisplayed() {
    when(timeService.shortDuration(any())).thenReturn("16min 40s");
    Replay replay = Instancio.of(Replay.class).set(field(Replay::replayTicks), 10000).create();

    runOnFxThreadAndWait(() -> instance.setReplay(replay));

    assertTrue(instance.replayDurationLabel.isVisible());
    assertTrue(instance.durationLabel.isVisible());

    assertEquals("16min 40s", instance.replayDurationLabel.getText());
  }

  @Test
  public void onDownloadMoreInfoClicked() throws Exception {
    Replay replay = Instancio.of(Replay.class).ignore(field(Replay::replayFile)).create();

    runOnFxThreadAndWait(() -> instance.setReplay(replay));
    WaitForAsyncUtils.waitForFxEvents();

    Path tmpPath = Path.of("foo.tmp");
    when(replayService.downloadReplay(replay.id())).thenReturn(CompletableFuture.completedFuture(tmpPath));

    runOnFxThreadAndWait(() -> instance.onDownloadMoreInfoClicked());

    verify(replayService, atLeastOnce()).loadReplayDetails(tmpPath);
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
    Replay onlineReplay = Instancio.create(Replay.class);

    runOnFxThreadAndWait(() -> {
      instance.setReplay(onlineReplay);
      instance.onWatchButtonClicked();
    });

    verify(replayService).runReplay(any(Replay.class));
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
    Replay replay = Instancio.create(Replay.class);
    ReplayReview review = Instancio.of(ReplayReview.class).set(field(ReplayReview::subject), replay).create();

    runOnFxThreadAndWait(() -> instance.setReplay(replay));

    when(reviewService.deleteReview(review)).thenReturn(Mono.empty());

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).deleteReview(review);
  }

  @Test
  public void testOnDeleteReviewThrowsException() {
    Replay replay = Instancio.create(Replay.class);
    ReplayReview review = Instancio.of(ReplayReview.class).set(field(ReplayReview::subject), replay).create();

    runOnFxThreadAndWait(() -> instance.setReplay(replay));

    when(reviewService.deleteReview(review)).thenReturn(Mono.error(new FakeTestException()));

    instance.onDeleteReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.delete.error"));
  }

  @Test
  public void testOnSendReviewNew() {
    Replay replay = Instancio.create(Replay.class);
    ReplayReview review = Instancio.of(ReplayReview.class)
                                   .ignore(field(ReplayReview::id))
                                   .set(field(ReplayReview::subject), replay)
                                   .create();

    runOnFxThreadAndWait(() -> instance.setReplay(replay));

    when(reviewService.saveReview(review)).thenReturn(Mono.empty());

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveReview(review);
  }

  @Test
  public void testOnSendReviewUpdate() {
    Replay replay = Instancio.create(Replay.class);
    ReplayReview review = Instancio.of(ReplayReview.class).set(field(ReplayReview::subject), replay).create();

    runOnFxThreadAndWait(() -> instance.setReplay(replay));

    when(reviewService.saveReview(review)).thenReturn(Mono.empty());

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(reviewService).saveReview(review);
  }

  @Test
  public void testOnSendReviewThrowsException() {
    Replay replay = Instancio.create(Replay.class);
    ReplayReview review = Instancio.of(ReplayReview.class).set(field(ReplayReview::subject), replay).create();

    runOnFxThreadAndWait(() -> instance.setReplay(replay));

    when(reviewService.saveReview(review)).thenReturn(Mono.error(new FakeTestException()));

    instance.onSendReview(review);
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addImmediateErrorNotification(any(), eq("review.save.error"));
  }

  @Test
  public void testReport() {
    Replay onlineReplay = Instancio.of(Replay.class).ignore(field(Replay::replayFile)).create();

    runOnFxThreadAndWait(() -> {
      instance.setReplay(onlineReplay);
      instance.onReport();
    });

    verify(reportDialogController).setReplay(onlineReplay);
    verify(reportDialogController).show();
  }

}
