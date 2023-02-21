package com.faforever.client.game;

import com.faforever.client.builders.FeaturedModBeanBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.JavaFxService;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratedEvent;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.replay.WatchButtonController;
import com.faforever.commons.lobby.GameStatus;
import com.google.common.eventbus.EventBus;
import javafx.animation.Animation.Status;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameDetailControllerTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private MapService mapService;
  @Mock
  private ModService modService;
  @Mock
  private JavaFxService javaFxService;
  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  @Mock
  private JoinGameHelper joinGameHelper;
  @Mock
  private TimeService timeService;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private EventBus eventBus;

  @Mock
  private WatchButtonController watchButtonController;
  @Mock
  private TeamCardController teamCardController;

  @InjectMocks
  private GameDetailController instance;
  private GameBean game;


  @BeforeEach
  public void setUp() throws Exception {
    game = GameBeanBuilder.create().defaultValues().get();

    when(javaFxService.getFxApplicationScheduler()).thenReturn(Schedulers.immediate());
    when(watchButtonController.gameProperty()).thenReturn(new SimpleObjectProperty<>());
    when(watchButtonController.getRoot()).thenReturn(new Button());
    when(teamCardController.getRoot()).then(invocation -> new Pane());
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(Mono.just(FeaturedModBeanBuilder.create()
        .defaultValues()
        .get()));
    when(mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE)).thenReturn(mock(Image.class));
    when(timeService.shortDuration(any())).thenReturn("duration");
    when(uiService.loadFxml("theme/team_card.fxml")).thenReturn(teamCardController);
    when(i18n.get("game.detail.players.format", game.getNumActivePlayers(), game.getMaxPlayers())).thenReturn(String.format("%d/%d", game.getNumActivePlayers(), game.getMaxPlayers()));
    when(i18n.get("unknown")).thenReturn("unknown");

    loadFxml("theme/play/game_detail.fxml", clazz -> {
      if (clazz == WatchButtonController.class) {
        return watchButtonController;
      }
      return instance;
    });
    runOnFxThreadAndWait(() -> instance.setGame(game));
  }

  @Test
  public void testSetGame() {
    assertTrue(instance.getRoot().isVisible());
    assertEquals(game.getTeams().size(), instance.teamListPane.getChildren().size());

    runOnFxThreadAndWait(() -> instance.setGame(null));
    assertEquals(0, instance.teamListPane.getChildren().size());
  }

  @Test
  public void testGameStatusListener() {
    assertFalse(instance.watchButton.isVisible());
    assertTrue(instance.joinButton.isVisible());
    runOnFxThreadAndWait(() -> game.setStatus(GameStatus.PLAYING));
    assertTrue(instance.watchButton.isVisible());
    assertFalse(instance.joinButton.isVisible());
    runOnFxThreadAndWait(() -> game.setStatus(GameStatus.CLOSED));
    assertFalse(instance.watchButton.isVisible());
    assertFalse(instance.joinButton.isVisible());
    runOnFxThreadAndWait(() -> game.setStatus(GameStatus.UNKNOWN));
    assertFalse(instance.watchButton.isVisible());
    assertFalse(instance.joinButton.isVisible());
    runOnFxThreadAndWait(() -> {
      game.setStatus(GameStatus.PLAYING);
      game.setStartTime(null);
    });
    assertFalse(instance.watchButton.isVisible());
    assertFalse(instance.joinButton.isVisible());

  }

  @Test
  public void testGamePropertyListener() {
    when(i18n.get("unknown")).thenReturn("unknown");

    assertEquals(game.getTitle(), instance.gameTitleLabel.getText());
    assertEquals(game.getHost(), instance.hostLabel.getText());
    assertEquals(game.getMapFolderName(), instance.mapLabel.getText());
    game.setTitle("blah");
    game.setHost("me");
    game.setMapFolderName("lala");
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(game.getTitle(), instance.gameTitleLabel.getText());
    assertEquals(game.getHost(), instance.hostLabel.getText());
    assertEquals(game.getMapFolderName(), instance.mapLabel.getText());
  }

  @Test
  public void testNumPlayersListener() {
    assertEquals(String.format("%d/%d", game.getNumActivePlayers(), game.getMaxPlayers()), instance.numberOfPlayersLabel.getText());
    when(i18n.get("game.detail.players.format", 2, 16)).thenReturn("2/16");
    runOnFxThreadAndWait(() -> {
      game.setTeams(Map.of(1, Set.of(1), 2, Set.of(2)));
      game.setMaxPlayers(16);
    });
    assertEquals(String.format("%d/%d", game.getNumActivePlayers(), game.getMaxPlayers()), instance.numberOfPlayersLabel.getText());
  }

  @Test
  public void testModListener() {
    assertEquals("Forged Alliance Forever", instance.gameTypeLabel.getText());
    FeaturedModBean mod = FeaturedModBeanBuilder.create()
        .defaultValues()
        .technicalName("ladder")
        .displayName("LADDER")
        .get();
    when(modService.getFeaturedMod(mod.getTechnicalName())).thenReturn(Mono.just(mod));
    runOnFxThreadAndWait(() -> game.setFeaturedMod(mod.getTechnicalName()));
    assertEquals(mod.getDisplayName(), instance.gameTypeLabel.getText());
  }

  @Test
  public void testTeamListener() {
    assertEquals(game.getTeams().size(), instance.teamListPane.getChildren().size());
    runOnFxThreadAndWait(() -> game.getTeams()
        .putAll(Map.of(1, Set.of(1), 2, Set.of(2))));
    assertEquals(game.getTeams().size(), instance.teamListPane.getChildren().size());
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
  }

  @Test
  public void testJoinGame() {
    instance.onJoinButtonClicked();
    verify(joinGameHelper).join(game);
  }

  @Test
  public void testNoPlaytimeWhenNoGame() {
    runOnFxThreadAndWait(() -> {
      instance.setPlaytimeVisible(true);
      instance.setGame(null);
    });
    assertFalse(instance.playtimeLabel.isVisible());
    assertNotSame(instance.getPlayTimeTimeline().getStatus(), Status.RUNNING);
  }

  @Test
  public void testNoPlaytimeWhenGameIsClosed() {
    runOnFxThreadAndWait(() -> {
      instance.setPlaytimeVisible(true);
      instance.setGame(GameBeanBuilder.create().defaultValues().status(GameStatus.CLOSED).get());
    });
    assertFalse(instance.playtimeLabel.isVisible());
    assertNotSame(instance.getPlayTimeTimeline().getStatus(), Status.RUNNING);
  }

  @Test
  public void testNoPlaytimeWhenPlayerIsInLobby() {
    runOnFxThreadAndWait(() -> {
      instance.setPlaytimeVisible(true);
      instance.setGame(GameBeanBuilder.create().defaultValues().status(GameStatus.OPEN).get());
    });
    assertFalse(instance.playtimeLabel.isVisible());
    assertNotSame(instance.getPlayTimeTimeline().getStatus(), Status.RUNNING);
  }

  @Test
  public void testShowPlaytimeWhenGameIsRunning() {
    runOnFxThreadAndWait(() -> {
      instance.setPlaytimeVisible(true);
      instance.setGame(GameBeanBuilder.create()
          .defaultValues()
          .status(GameStatus.PLAYING)
          .startTime(OffsetDateTime.now())
          .get());
    });
    assertTrue(instance.playtimeLabel.isVisible());
    assertSame(Status.RUNNING, instance.getPlayTimeTimeline().getStatus());
  }

  @Test
  public void testHidePlaytimeWhenGameHasJustEnded() throws Exception {
    GameBean game = GameBeanBuilder.create()
        .defaultValues()
        .status(GameStatus.PLAYING)
        .startTime(OffsetDateTime.now())
        .get();

    runOnFxThreadAndWait(() -> {
      instance.setPlaytimeVisible(true);
      instance.setGame(game);
    });
    Thread.sleep(2000);
    assertSame(Status.RUNNING, instance.getPlayTimeTimeline().getStatus());
    runOnFxThreadAndWait(() -> game.setStatus(GameStatus.CLOSED));
    Thread.sleep(1000);

    assertFalse(instance.playtimeLabel.isVisible());
    assertSame(Status.STOPPED, instance.getPlayTimeTimeline().getStatus());
  }

  @Test
  public void testGenerateMapButtonIsVisible() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(mapGeneratorService.isGeneratedMap(game.getMapFolderName())).thenReturn(true);
    when(mapService.isInstalled(game.getMapFolderName())).thenReturn(false);

    runOnFxThreadAndWait(() -> instance.setGame(game));

    assertTrue(instance.generateMapButton.isVisible());
  }

  @Test
  public void testGenerateMapButtonIsInvisible() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(mapGeneratorService.isGeneratedMap(game.getMapFolderName())).thenReturn(false);

    runOnFxThreadAndWait(() -> instance.setGame(game));

    assertFalse(instance.generateMapButton.isVisible());
  }

  @Test
  public void testGenerateMapButtonIsInvisibleWhenMapIsInstalled() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(mapGeneratorService.isGeneratedMap(game.getMapFolderName())).thenReturn(true);
    when(mapService.isInstalled(game.getMapFolderName())).thenReturn(true);

    runOnFxThreadAndWait(() -> instance.setGame(game));

    assertFalse(instance.generateMapButton.isVisible());
  }

  @Test
  public void testOnGenerateMapClickedAndSucceed() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    Image noImageMock = mock(Image.class);
    Image imageMock = mock(Image.class);
    when(mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE)).thenReturn(noImageMock, imageMock);
    when(mapGeneratorService.isGeneratedMap(game.getMapFolderName())).thenReturn(true);
    when(mapService.isInstalled(game.getMapFolderName())).thenReturn(false);
    when(mapService.generateIfNotInstalled(game.getMapFolderName())).thenReturn(CompletableFuture.completedFuture(game.getMapFolderName()));

    runOnFxThreadAndWait(() -> {
      instance.setGame(game);
      instance.onGenerateMapClicked();
    });
    verify(mapService).generateIfNotInstalled(game.getMapFolderName());
    assertEquals(noImageMock, instance.mapImageView.getImage());

    when(mapService.isInstalled(game.getMapFolderName())).thenReturn(true);
    runOnFxThreadAndWait(() -> instance.onMapGeneratedEvent(new MapGeneratedEvent(game.getMapFolderName())));

    assertEquals(instance.mapImageView.getImage(), imageMock);
    assertFalse(instance.generateMapButton.isVisible());
  }

  @Test
  public void testOnGenerateMapClickedAndFailed() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(mapGeneratorService.isGeneratedMap(game.getMapFolderName())).thenReturn(true);
    when(mapService.isInstalled(game.getMapFolderName())).thenReturn(false);
    when(mapService.generateIfNotInstalled(game.getMapFolderName())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("failed")));

    runOnFxThreadAndWait(() -> {
      instance.setGame(game);
      instance.onGenerateMapClicked();
    });

    assertTrue(instance.generateMapButton.isVisible());
    verify(notificationService).addImmediateErrorNotification(any(RuntimeException.class), any(String.class));
  }

  @Test
  public void testOnGenerateMapClickedAndInProcess() {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(i18n.get("game.mapGeneration.notification.title" )).thenReturn("in process");

    when(mapService.generateIfNotInstalled(game.getMapFolderName())).thenAnswer(invocation -> {
      assertEquals("in process", instance.generateMapButton.getText());
      assertTrue(instance.generateMapButton.isDisable());
      return CompletableFuture.completedFuture(game.getMapFolderName());
    });

    runOnFxThreadAndWait(() -> {
      instance.setGame(game);
      instance.onGenerateMapClicked();
    });

    verify(mapService).generateIfNotInstalled(game.getMapFolderName());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void testOnGenerateMapClickedAndCompleted(boolean succeed) {
    GameBean game = GameBeanBuilder.create().defaultValues().get();
    when(i18n.get("game.create.generatedMap")).thenReturn("text");
    when(mapService.generateIfNotInstalled(game.getMapFolderName())).thenReturn(
        succeed ? CompletableFuture.completedFuture(game.getMapFolderName()) : CompletableFuture.failedFuture(new RuntimeException("failed")));

    runOnFxThreadAndWait(() -> {
      instance.setGame(game);
      instance.onGenerateMapClicked();
    });

    assertFalse(instance.generateMapButton.isDisable());
    assertEquals("text", instance.generateMapButton.getText());
  }

  @Test
  public void testOnGenerateMapClickedAndDoNotSetImageIfGameIsAnother() {
    GameBean game = GameBeanBuilder.create().defaultValues().mapFolderName("neroxis").get();
    GameBean anotherGame = GameBeanBuilder.create().defaultValues().mapFolderName("non_neroxis").get();
    Image image = mock(Image.class);
    Image anotherImage = mock(Image.class);
    when(mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE)).thenReturn(image);
    when(mapService.loadPreview(anotherGame.getMapFolderName(), PreviewSize.LARGE)).thenReturn(anotherImage);

    when(mapService.generateIfNotInstalled(game.getMapFolderName()))
        .thenAnswer(invocation -> {
          assertEquals(image, instance.mapImageView.getImage());
          runOnFxThreadAndWait(() -> instance.setGame(anotherGame));
          return CompletableFuture.completedFuture(game.getMapFolderName());
        });

    runOnFxThreadAndWait(() -> {
      instance.setGame(game);
      instance.onGenerateMapClicked();
    });

    verify(mapService).generateIfNotInstalled(game.getMapFolderName());
    assertEquals(anotherImage, instance.mapImageView.getImage());
  }
}
