package com.faforever.client.replay;

import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.Replay;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.game.PlayerCardController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ReplayHistoryPrefs;
import com.faforever.client.rating.RatingService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.StarsController;
import com.faforever.commons.api.dto.Validity;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.image.Image;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.instancio.Select.field;
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
  private Consumer<Replay> onOpenDetailListener;

  @Spy
  private ReplayHistoryPrefs replayHistory;

  private Replay onlineReplay;
  private Replay localReplay;
  private MapVersion mapBean;

  private final BooleanProperty installed = new SimpleBooleanProperty();

  @BeforeEach
  public void setUp() throws Exception {
    mapBean = Instancio.create(MapVersion.class);
    onlineReplay = Instancio.of(Replay.class)
                            .set(field(Replay::validity), Validity.VALID)
                            .set(field(Replay::title), "test")
                            .set(field(Replay::mapVersion), mapBean)
                            .ignore(field(Replay::replayFile))
                            .create();
    localReplay = Instancio.of(Replay.class)
                           .set(field(Replay::local), true)
                           .set(field(Replay::title), "test")
                           .set(field(Replay::mapVersion), mapBean)
                           .set(field(Replay::replayFile), Path.of("foo.tmp"))
                           .ignore(field(Replay::validity))
                           .create();
    lenient().when(uiService.loadFxml("theme/player_card.fxml")).thenReturn(playerCardController);
    lenient().when(replayService.loadReplayDetails(any())).thenReturn(new ReplayDetails(List.of(), List.of(), mapBean));
    lenient().when(mapService.isInstalledBinding(Mockito.<MapVersion>any())).thenReturn(installed);
    lenient().when(mapService.loadPreview(anyString(), eq(PreviewSize.LARGE)))
             .thenReturn(new Image(InputStream.nullInputStream()));
    lenient().when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());
    lenient().when(starsController.valueProperty()).thenReturn(new SimpleFloatProperty());
    lenient().when(timeService.asDate(onlineReplay.startTime())).thenReturn("Min Date");
    lenient().when(timeService.asShortTime(onlineReplay.startTime())).thenReturn("Min Time");
    lenient().when(timeService.asDate(localReplay.startTime())).thenReturn("Min Date");
    lenient().when(timeService.asShortTime(localReplay.startTime())).thenReturn("Min Time");
    lenient().when(timeService.shortDuration(any(Duration.class))).thenReturn("Forever");
    lenient().when(i18n.get("game.onUnknownMap")).thenReturn("unknown map");
    lenient().when(i18n.get("unknown")).thenReturn("unknown");
    lenient().when(i18n.number(anyInt())).thenReturn("1234");
    lenient().when(i18n.get(eq("game.idFormat"), anyInt()))
             .thenAnswer(invocation -> invocation.getArgument(1).toString());
    lenient().when(i18n.get(eq("game.onMapFormat"), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
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

    verify(mapService).loadPreview(mapBean.folderName(), PreviewSize.SMALL);
    assertTrue(instance.teamsContainer.isVisible());
    assertTrue(instance.mapThumbnailImageView.isVisible());
    assertTrue(instance.gameTitleLabel.isVisible());
    assertTrue(instance.replayTileRoot.isVisible());
    assertTrue(instance.timeLabel.isVisible());
    assertTrue(instance.modLabel.isVisible());
    assertTrue(instance.tickDurationLabel.isVisible());
    assertTrue(instance.tickDurationLabel.isManaged());
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
    assertEquals(onlineReplay.featuredMod().displayName(), instance.modLabel.getText());
    assertEquals("1234", instance.playerCountLabel.getText());
    assertEquals("42", instance.qualityLabel.getText());
    assertEquals(mapBean.map().displayName(), instance.onMapLabel.getText());
    assertEquals(String.valueOf(onlineReplay.id()), instance.replayIdField.getText());
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
    assertTrue(instance.tickDurationLabel.isVisible());
    assertTrue(instance.tickDurationLabel.isManaged());
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
    assertEquals(localReplay.featuredMod().displayName(), instance.modLabel.getText());
    assertEquals("1234", instance.playerCountLabel.getText());
    assertEquals(mapBean.map().displayName(), instance.onMapLabel.getText());
    assertEquals(String.valueOf(localReplay.id()), instance.replayIdField.getText());
  }

  @Test
  public void setReplayNoEndTime() {
    Replay onlineReplay = Instancio.of(Replay.class)
                                   .ignore(field(Replay::endTime))
                                   .ignore(field(Replay::replayTicks))
                                   .create();

    runOnFxThreadAndWait(() -> instance.setEntity(onlineReplay));

    assertFalse(instance.realTimeDurationLabel.isVisible());
    assertFalse(instance.tickDurationLabel.isVisible());
  }

  @Test
  public void tickTimeDisplayedWhenDefinedTogetherWithRealTime(){
    Replay onlineReplay = Instancio.of(Replay.class).set(field(Replay::replayTicks), 1000).create();
    runOnFxThreadAndWait(() -> instance.setEntity(onlineReplay));

    assertTrue(instance.realTimeDurationLabel.isVisible());
    assertTrue(instance.tickDurationLabel.isVisible());
  }

  @Test
  public void tickTimeNotDisplayWhenNotDefined() {
    Replay onlineReplay = Instancio.of(Replay.class)
                                   .ignore(field(Replay::endTime))
                                   .set(field(Replay::replayTicks), null)
                                   .create();
    runOnFxThreadAndWait(() -> instance.setEntity(onlineReplay));

    assertFalse(instance.tickDurationLabel.isVisible());
  }

  @Test
  public void setReplayNoTeamStats() {
    Replay onlineReplay = Instancio.of(Replay.class).ignore(field(Replay::teamPlayerStats)).create();

    runOnFxThreadAndWait(() -> instance.setEntity(onlineReplay));
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals("-", instance.ratingLabel.getText());
  }

  @Test
  public void setReplayMissing() {
    Replay onlineReplay = Instancio.of(Replay.class)
                                   .ignore(field(Replay::replayAvailable))
                                   .set(field(Replay::startTime), OffsetDateTime.now().minusDays(2))
                                   .create();

    runOnFxThreadAndWait(() -> instance.setEntity(onlineReplay));

    assertTrue(instance.watchButton.isDisabled());
  }

  @Test
  public void testOnWatchButtonClicked() {
    runOnFxThreadAndWait(() -> {
      instance.setEntity(onlineReplay);
      instance.onWatchButtonClicked();
    });

    verify(replayService).runReplay(any(Replay.class));
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

  @Test
  public void replayNotWatched() {
    runOnFxThreadAndWait(() -> instance.setEntity(localReplay));

    assertFalse(instance.replayTileRoot.getPseudoClassStates().contains(ReplayCardController.WATCHED_PSEUDO_CLASS));
  }

  @Test
  public void replayWatched() {

    runOnFxThreadAndWait(() -> {
      replayHistory.getWatchedReplays().add(localReplay.id());
      instance.setEntity(localReplay);
    });

    assertTrue(instance.replayTileRoot.getPseudoClassStates().contains(ReplayCardController.WATCHED_PSEUDO_CLASS));
  }


  @Test
  public void replayAfterInitJustWatched() {

    runOnFxThreadAndWait(() -> {
      instance.setEntity(localReplay);
      replayHistory.getWatchedReplays().add(localReplay.id());
    });


    assertTrue(instance.replayTileRoot.getPseudoClassStates().contains(ReplayCardController.WATCHED_PSEUDO_CLASS));
  }
}
