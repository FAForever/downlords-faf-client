package com.faforever.client.replay;

import com.faforever.client.builders.MapBeanBuilder;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.builders.PlayerStatsMapBuilder;
import com.faforever.client.builders.ReplayBeanBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.game.PlayerCardController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.rating.RatingService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.StarsController;
import com.faforever.commons.api.dto.Validity;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReplayCardControllerTest extends PlatformTest {
  @InjectMocks
  private ReplayCardController instance;

  @Mock
  private UiService uiService;

  @Mock
  private ReplayService replayService;

  @Mock
  private TimeService timeService;

  @Mock
  private MapService mapService;

  @Mock
  private RatingService ratingService;

  @Mock
  private NotificationService notificationService;

  @Mock
  private ImageViewHelper imageViewHelper;

  @Mock
  private I18n i18n;

  @Mock
  private FxApplicationThreadExecutor fxApplicationThreadExecutor;

  @Mock
  private PlayerCardController playerCardController;

  @Mock
  private StarsController starsController;

  @Mock
  private Consumer<ReplayBean> onOpenDetailListener;

  private ReplayBean onlineReplay;
  private ReplayBean localReplay;
  private MapVersionBean mapBean;

  private final BooleanProperty installed = new SimpleBooleanProperty();

  @BeforeEach
  public void setUp() throws Exception {
    mapBean = MapVersionBeanBuilder.create().defaultValues().map(MapBeanBuilder.create().defaultValues().get()).get();
    onlineReplay = ReplayBeanBuilder.create()
                                    .defaultValues()
                                    .validity(Validity.VALID)
                                    .featuredMod(new FeaturedModBean())
                                    .title("test")
                                    .mapVersion(mapBean)
                                    .teamPlayerStats(PlayerStatsMapBuilder.create().defaultValues().get())
                                    .get();
    localReplay = ReplayBeanBuilder.create()
                                   .defaultValues()
                                   .local(true)
                                   .validity(null)
                                   .featuredMod(new FeaturedModBean())
                                   .title("test")
                                   .mapVersion(mapBean)
                                   .replayFile(Path.of("foo.tmp"))
                                   .get();
    lenient().when(uiService.loadFxml("theme/player_card.fxml")).thenReturn(playerCardController);
    lenient().when(replayService.loadReplayDetails(any())).thenReturn(new ReplayDetails(List.of(), List.of(), mapBean));
    lenient().when(mapService.isInstalledBinding(Mockito.<MapVersionBean>any())).thenReturn(installed);
    lenient().when(mapService.loadPreview(anyString(), eq(PreviewSize.LARGE)))
             .thenReturn(new Image(InputStream.nullInputStream()));
    lenient().when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());
    lenient().when(starsController.valueProperty()).thenReturn(new SimpleFloatProperty());
    lenient().when(timeService.asDate(onlineReplay.getStartTime())).thenReturn("Min Date");
    lenient().when(timeService.asShortTime(onlineReplay.getStartTime())).thenReturn("Min Time");
    lenient().when(timeService.asDate(localReplay.getStartTime())).thenReturn("Min Date");
    lenient().when(timeService.asShortTime(localReplay.getStartTime())).thenReturn("Min Time");
    lenient().when(timeService.shortDuration(any(Duration.class))).thenReturn("Forever");
    lenient().when(i18n.get("game.onUnknownMap")).thenReturn("unknown map");
    lenient().when(i18n.get("unknown")).thenReturn("unknown");
    lenient().when(i18n.number(anyInt())).thenReturn("1234");
    lenient().when(i18n.get("game.idFormat", onlineReplay.getId())).thenReturn(String.valueOf(onlineReplay.getId()));
    lenient().when(i18n.get("game.onMapFormat", mapBean.getMap().getDisplayName()))
             .thenReturn(mapBean.getMap().getDisplayName());
    loadFxml("theme/vault/replay/replay_card.fxml", param -> {
      if (param == StarsController.class) {
        return starsController;
      }
      return instance;
    });
  }

  @Test
  public void setReplayOnline() {
    when(ratingService.calculateQuality(onlineReplay)).thenReturn(0.427);
    when(i18n.get(eq("percentage"), eq(Math.round(0.427 * 100)))).thenReturn("42");

    runOnFxThreadAndWait(() -> instance.setEntity(onlineReplay));

    verify(mapService).loadPreview(mapBean.getFolderName(), PreviewSize.SMALL);
    assertTrue(instance.teamsContainer.isVisible());
    assertTrue(instance.mapThumbnailImageView.isVisible());
    assertTrue(instance.gameTitleLabel.isVisible());
    assertTrue(instance.replayTileRoot.isVisible());
    assertTrue(instance.timeLabel.isVisible());
    assertTrue(instance.modLabel.isVisible());
    assertFalse(instance.tickDurationLabel.isVisible());
    assertFalse(instance.tickDurationLabel.isManaged());
    assertTrue(instance.realTimeDurationLabel.isVisible());
    assertTrue(instance.realTimeDurationLabel.isManaged());
    assertTrue(instance.playerCountLabel.isVisible());
    assertTrue(instance.ratingLabel.isVisible());
    assertTrue(instance.qualityLabel.isVisible());
    assertTrue(instance.numberOfReviewsLabel.isVisible());
    assertTrue(instance.onMapLabel.isVisible());
    assertTrue(instance.watchButton.isVisible());
    assertFalse(instance.deleteButton.isVisible());
    assertFalse(instance.deleteButton.isManaged());
    assertEquals("-", instance.ratingLabel.getText());
    assertEquals("Min Date", instance.dateLabel.getText());
    assertEquals("Min Time", instance.timeLabel.getText());
    assertEquals(null, instance.modLabel.getText());
    assertEquals("1234", instance.playerCountLabel.getText());
    assertEquals("42", instance.qualityLabel.getText());
    assertEquals(mapBean.getMap().getDisplayName(), instance.onMapLabel.getText());
    assertEquals(String.valueOf(onlineReplay.getId()), instance.replayIdField.getText());
  }

  @Test
  public void setReplayLocal() throws Exception {
    runOnFxThreadAndWait(() -> instance.setEntity(localReplay));

    assertTrue(instance.teamsContainer.isVisible());
    assertTrue(instance.mapThumbnailImageView.isVisible());
    assertTrue(instance.gameTitleLabel.isVisible());
    assertTrue(instance.replayTileRoot.isVisible());
    assertTrue(instance.timeLabel.isVisible());
    assertTrue(instance.modLabel.isVisible());
    assertFalse(instance.tickDurationLabel.isVisible());
    assertFalse(instance.tickDurationLabel.isManaged());
    assertTrue(instance.realTimeDurationLabel.isVisible());
    assertTrue(instance.realTimeDurationLabel.isManaged());
    assertTrue(instance.playerCountLabel.isVisible());
    assertTrue(instance.ratingLabel.isVisible());
    assertTrue(instance.qualityLabel.isVisible());
    assertTrue(instance.numberOfReviewsLabel.isVisible());
    assertTrue(instance.onMapLabel.isVisible());
    assertTrue(instance.watchButton.isVisible());
    assertTrue(instance.deleteButton.isVisible());
    assertTrue(instance.deleteButton.isManaged());
    assertEquals("-", instance.ratingLabel.getText());
    assertEquals("Min Date", instance.dateLabel.getText());
    assertEquals("Min Time", instance.timeLabel.getText());
    assertEquals(null, instance.modLabel.getText());
    assertEquals("1234", instance.playerCountLabel.getText());
    assertEquals(mapBean.getMap().getDisplayName(), instance.onMapLabel.getText());
    assertEquals(String.valueOf(localReplay.getId()), instance.replayIdField.getText());
  }

  @Test
  public void setReplayNoEndTime() {
    onlineReplay.setEndTime(null);

    runOnFxThreadAndWait(() -> instance.setEntity(onlineReplay));

    assertFalse(instance.realTimeDurationLabel.isVisible());
    assertFalse(instance.tickDurationLabel.isVisible());
  }

  @Test
  public void tickTimeNotDisplayedWhenRealTimeIs() {
    onlineReplay.setReplayTicks(1000);
    runOnFxThreadAndWait(() -> instance.setEntity(onlineReplay));

    assertTrue(instance.realTimeDurationLabel.isVisible());
    assertFalse(instance.tickDurationLabel.isVisible());
  }

  @Test
  public void tickTimeDisplayedWhenRealTimeIsNot() {
    onlineReplay.setEndTime(null);
    onlineReplay.setReplayTicks(1000);
    runOnFxThreadAndWait(() -> instance.setEntity(onlineReplay));

    assertFalse(instance.realTimeDurationLabel.isVisible());
    assertTrue(instance.tickDurationLabel.isVisible());
  }

  @Test
  public void setReplayNoTeamStats() {
    onlineReplay.setTeamPlayerStats(FXCollections.emptyObservableMap());

    runOnFxThreadAndWait(() -> instance.setEntity(onlineReplay));
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("-", instance.ratingLabel.getText());
  }

  @Test
  public void setReplayMissing() {
    onlineReplay.setReplayAvailable(false);
    onlineReplay.setStartTime(OffsetDateTime.now().minusDays(2));

    when(i18n.get("game.replayFileMissing")).thenReturn("missing");

    runOnFxThreadAndWait(() -> instance.setEntity(onlineReplay));

    assertTrue(instance.watchButton.isDisabled());
  }

  @Test
  public void testOnWatchButtonClicked() {
    runOnFxThreadAndWait(() -> instance.setEntity(onlineReplay));

    instance.onWatchButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(replayService).runReplay(onlineReplay);
  }

  @Test
  public void testReplayOpened() {
    instance.setOnOpenDetailListener(onOpenDetailListener);
    runOnFxThreadAndWait(() -> instance.setEntity(onlineReplay));

    instance.onShowReplayDetail();
    WaitForAsyncUtils.waitForFxEvents();

    verify(onOpenDetailListener).accept(any());
  }

  @Test
  public void deleteButtonClicked() {
    runOnFxThreadAndWait(() -> instance.setEntity(localReplay));

    instance.onDeleteButtonClicked();
    WaitForAsyncUtils.waitForFxEvents();

    verify(notificationService).addNotification(any());
  }
}