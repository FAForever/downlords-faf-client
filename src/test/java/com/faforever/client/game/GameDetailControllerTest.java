package com.faforever.client.game;

import com.faforever.client.builders.FeaturedModBeanBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.WatchButtonController;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.commons.lobby.GameStatus;
import javafx.animation.Animation.Status;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameDetailControllerTest extends PlatformTest {

  @Mock
  private I18n i18n;
  @Mock
  private MapService mapService;
  @Mock
  private ModService modService;
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
  private ImageViewHelper imageViewHelper;

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

    doAnswer(invocation -> new SimpleObjectProperty<>(invocation.getArgument(0))).when(imageViewHelper)
        .createPlaceholderImageOnErrorObservable(any());
    when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());
    when(watchButtonController.gameProperty()).thenReturn(new SimpleObjectProperty<>());
    when(watchButtonController.getRoot()).thenReturn(new Button());
    when(teamCardController.getRoot()).then(invocation -> new Pane());
    when(teamCardController.ratingProviderProperty()).thenReturn(new SimpleObjectProperty<>());
    when(teamCardController.playerIdsProperty()).thenReturn(new SimpleObjectProperty<>());
    when(teamCardController.teamIdProperty()).thenReturn(new SimpleIntegerProperty());
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(Mono.just(FeaturedModBeanBuilder.create()
        .defaultValues()
        .get()));
    when(mapService.isInstalledBinding(anyString())).thenReturn(new SimpleBooleanProperty());
    when(mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE)).thenReturn(new Image(InputStream.nullInputStream()));
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
    assertEquals(game.getTeams().size(), instance.teamListPane.getChildren().stream().filter(Node::isVisible).count());

    runOnFxThreadAndWait(() -> instance.setGame(null));
    assertTrue(instance.teamListPane.getChildren().stream().noneMatch(Node::isVisible));
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
    runOnFxThreadAndWait(() -> {
      game.setTitle("blah");
      game.setHost("me");
      game.setMapFolderName("lala");
    });
    assertEquals(game.getTitle(), instance.gameTitleLabel.getText());
    assertEquals(game.getHost(), instance.hostLabel.getText());
    assertEquals(game.getMapFolderName(), instance.mapLabel.getText());
  }

  @Test
  public void testNumPlayersListener() {
    assertEquals(String.format("%d/%d", game.getNumActivePlayers(), game.getMaxPlayers()), instance.numberOfPlayersLabel.getText());
    when(i18n.get("game.detail.players.format", 2, 16)).thenReturn("2/16");
    runOnFxThreadAndWait(() -> {
      game.setTeams(Map.of(1, List.of(1), 2, List.of(2)));
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
    assertEquals(game.getTeams().size(), instance.teamListPane.getChildren().stream().filter(Node::isVisible).count());
    runOnFxThreadAndWait(() -> game.getTeams().putAll(Map.of(1, List.of(1), 2, List.of(2))));
    assertEquals(game.getTeams().size(), instance.teamListPane.getChildren().stream().filter(Node::isVisible).count());
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
    assertEquals(Status.RUNNING, instance.getPlayTimeTimeline().getStatus());
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
    assertEquals(Status.RUNNING, instance.getPlayTimeTimeline().getStatus());
    runOnFxThreadAndWait(() -> game.setStatus(GameStatus.CLOSED));
    Thread.sleep(2000);

    assertFalse(instance.playtimeLabel.isVisible());
    assertEquals(Status.STOPPED, instance.getPlayTimeTimeline().getStatus());
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
    when(i18n.get("game.mapGeneration.notification.title")).thenReturn("in process");

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
    when(mapService.generateIfNotInstalled(game.getMapFolderName())).thenReturn(succeed ? CompletableFuture.completedFuture(game.getMapFolderName()) : CompletableFuture.failedFuture(new RuntimeException("failed")));

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
    Image image = new Image(InputStream.nullInputStream());
    Image anotherImage = new Image(InputStream.nullInputStream());
    when(mapService.loadPreview(game.getMapFolderName(), PreviewSize.SMALL)).thenReturn(image);
    when(mapService.loadPreview(anotherGame.getMapFolderName(), PreviewSize.SMALL)).thenReturn(anotherImage);

    when(mapService.generateIfNotInstalled(game.getMapFolderName())).thenAnswer(invocation -> {
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
