package com.faforever.client.replay;

import com.faforever.client.builders.MapBeanBuilder;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PlayerStatsMapBuilder;
import com.faforever.client.builders.ReplayBeanBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.game.TeamCardController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.player.PlayerService;
import com.faforever.client.rating.RatingService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.ReviewController;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import com.faforever.commons.api.dto.Validity;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

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

public class ReplayCardControllerTest extends UITest {

  @InjectMocks
  private ReplayCardController instance;

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
  private ReportDialogController reportDialogController;

  private ReplayBean onlineReplay;
  private MapVersionBean mapBean;

  @BeforeEach
  public void setUp() throws Exception {
    mapBean = MapVersionBeanBuilder.create().defaultValues().map(MapBeanBuilder.create().defaultValues().get()).get();
    onlineReplay = ReplayBeanBuilder.create().defaultValues()
        .validity(Validity.VALID)
        .featuredMod(new FeaturedModBean())
        .reviews(FXCollections.emptyObservableList())
        .title("test")
        .mapVersion(mapBean)
        .teamPlayerStats(PlayerStatsMapBuilder.create().defaultValues().get())
        .get();

    ReplayBean localReplay = ReplayBeanBuilder.create().defaultValues()
        .validity(null)
        .featuredMod(new FeaturedModBean())
        .reviews(FXCollections.emptyObservableList())
        .title("test")
        .replayFile(Path.of("foo.tmp"))
        .get();

    when(reviewsController.getRoot()).thenReturn(new Pane());
    when(mapService.loadPreview(anyString(), eq(PreviewSize.LARGE))).thenReturn(mock(Image.class));
    when(playerService.getCurrentPlayer()).thenReturn(PlayerBeanBuilder.create().defaultValues().get());
    when(playerService.getPlayersByIds(any())).thenReturn(CompletableFuture.completedFuture(List.of(PlayerBeanBuilder.create().defaultValues().get())));
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

    loadFxml("theme/vault/replay/replay_card.fxml", param -> {
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

    instance.setReplay(onlineReplay);
    WaitForAsyncUtils.waitForFxEvents();

    verify(mapService).loadPreview(mapBean.getFolderName(), PreviewSize.SMALL);
    assertTrue(instance.dateLabel.isVisible());
    assertTrue(instance.durationLabel.isVisible());
    assertTrue(instance.gameTitleLabel.isVisible());
    assertTrue(instance.modLabel.isVisible());
    assertTrue(instance.playerCountLabel.isVisible());
    assertFalse(instance.watchButton.isDisabled());
    assertFalse(instance.deleteButton.isVisible());
    assertFalse(instance.deleteButton.isManaged());
    assertNotEquals("-", instance.ratingLabel.getText());
    assertEquals("Min Date", instance.dateLabel.getText());
    assertEquals("Min Time", instance.timeLabel.getText());
    assertEquals("Forever", instance.durationLabel.getText());
    assertEquals(null, instance.modLabel.getText());
    assertEquals("test", instance.gameTitleLabel.getText());
    assertEquals("1234", instance.playerCountLabel.getText());
    assertEquals("42", instance.qualityLabel.getText());
    assertEquals(mapBean.getMap().getDisplayName(), instance.onMapLabel.getText());
  }

  @Test
  public void setReplayNoEndTime() {
    onlineReplay.setEndTime(null);

    instance.setReplay(onlineReplay);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.durationLabel.isVisible());
  }

  @Test
  public void setReplayNoTeamStats() {
    onlineReplay.setTeamPlayerStats(FXCollections.emptyObservableMap());

    instance.setReplay(onlineReplay);
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("-", instance.ratingLabel.getText());
  }

  @Test
  public void setReplayMissing() {
    onlineReplay.setReplayAvailable(false);
    onlineReplay.setStartTime(OffsetDateTime.now().minusDays(2));

    when(i18n.get("game.replayFileMissing")).thenReturn("missing");

    instance.setReplay(onlineReplay);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.watchButton.isDisabled());
    assertTrue(instance.hostButton.isDisabled());
  }

  @Test
  public void setReplayNotAvailable() {
    onlineReplay.setReplayAvailable(false);

    when(i18n.get("game.replayNotAvailable")).thenReturn("not available");

    instance.setReplay(onlineReplay);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.watchButton.isDisabled());
    assertTrue(instance.hostButton.isDisabled());
  }

  @Test
  public void tickTimeDisplayed() {
    when(replayService.getFileSize(any())).thenReturn(CompletableFuture.completedFuture(1024));
    when(timeService.shortDuration(any())).thenReturn("16min 40s");
    ReplayBean replay = ReplayBeanBuilder.create().defaultValues().replayTicks(10_000).get();

    instance.setReplay(replay);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.durationLabel.isVisible());
    assertTrue(instance.durationLabel.isVisible());

    assertEquals("16min 40s", instance.durationLabel.getText());
  }

  @Test
  public void testOnWatchButtonClicked() {
    instance.setReplay(onlineReplay);
    instance.onWatchButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(replayService).runReplay(onlineReplay);
  }

  @Test
  public void testOnHostButtonClicked() {
    instance.setReplay(onlineReplay);
    instance.onHostButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(replayService).hostFromReplay(onlineReplay);
  }

}
