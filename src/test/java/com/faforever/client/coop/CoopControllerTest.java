package com.faforever.client.coop;

import com.faforever.client.builders.CoopResultBeanBuilder;
import com.faforever.client.builders.FeaturedModBeanBuilder;
import com.faforever.client.builders.ReplayBeanBuilder;
import com.faforever.client.domain.CoopMissionBean;
import com.faforever.client.domain.CoopResultBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.game.GamesTableController;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import javafx.collections.FXCollections;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.faforever.client.game.KnownFeaturedMod.COOP;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoopControllerTest extends PlatformTest {

  @Mock
  private CoopService coopService;
  @Mock
  private GameService gameService;
  @Mock
  private UiService uiService;
  @Mock
  private ImageViewHelper imageViewHelper;
  @Mock
  private GamesTableController gamesTableController;
  @Mock
  private GameTooltipController gameTooltipController;
  @Mock
  private I18n i18n;
  @Mock
  private WebViewConfigurer webViewConfigurer;
  @Mock
  private ModService modService;
  @Mock
  private ReplayService replayService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private TimeService timeService;

  @InjectMocks
  private CoopController instance;

  @BeforeEach
  public void setUp() throws Exception {
    when(coopService.getLeaderboard(any(), anyInt())).thenReturn(completedFuture(emptyList()));
    when(coopService.getMissions()).thenReturn(completedFuture(emptyList()));
    when(modService.getFeaturedMod(COOP.getTechnicalName())).thenReturn(Mono.just(FeaturedModBeanBuilder.create().defaultValues().technicalName("coop").get()));
    when(gameService.getGames()).thenReturn(FXCollections.emptyObservableList());
    when(uiService.loadFxml("theme/play/games_table.fxml")).thenReturn(gamesTableController);
    when(gamesTableController.getRoot()).thenReturn(new Pane());

    loadFxml("theme/play/coop/coop.fxml", clazz -> {
      if (GamesTableController.class == clazz) {
        return gamesTableController;
      }

      if (GameTooltipController.class == clazz) {
        return gameTooltipController;
      }

      return instance;
    });

    verify(webViewConfigurer).configureWebView(instance.descriptionWebView);
  }

  @Test
  public void onPlayButtonClicked() {
    when(coopService.getMissions()).thenReturn(completedFuture(singletonList(new CoopMissionBean())));
    runOnFxThreadAndWait(() -> reinitialize(instance));

    instance.missionComboBox.getSelectionModel().select(new CoopMissionBean());

    WaitForAsyncUtils.waitForFxEvents();
    instance.onPlayButtonClicked();

    ArgumentCaptor<NewGameInfo> captor = ArgumentCaptor.forClass(NewGameInfo.class);
    verify(gameService).hostGame(captor.capture());

    NewGameInfo newGameInfo = captor.getValue();
    assertEquals("coop", newGameInfo.getFeaturedMod().getTechnicalName());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.coopRoot, instance.getRoot());
    assertNull(instance.getRoot().getParent());
  }

  @Test
  public void testNoDuplicatedPlayersInTableWhenSetCountPlayersToOne() {
    List<CoopResultBean> result = new ArrayList<>();
    result.add(CoopResultBeanBuilder.create().defaultValues()
        .replay(ReplayBeanBuilder.create().defaultValues()
            .teams(FXCollections.observableMap(Map.of("2", List.of("junit1"))))
            .get())
        .get());

    result.add(CoopResultBeanBuilder.create().defaultValues()
        .replay(ReplayBeanBuilder.create().defaultValues()
            .teams(FXCollections.observableMap(Map.of("2", List.of("junit1"))))
            .get())
        .get());

    result.add(CoopResultBeanBuilder.create().defaultValues()
        .replay(ReplayBeanBuilder.create().defaultValues()
            .teams(FXCollections.observableMap(Map.of("2", List.of("junit2"))))
            .get())
        .get());

    when(coopService.getLeaderboard(any(), eq(1))).thenReturn(completedFuture(result));

    instance.missionComboBox.getSelectionModel().select(new CoopMissionBean());

    runOnFxThreadAndWait(() -> {
      reinitialize(instance);
      instance.numberOfPlayersComboBox.getSelectionModel().select(1);
    });
    assertEquals(2, instance.leaderboardTable.getItems().size());
  }

  @Test
  public void testNoDuplicatedPlayersInTableWhenSetCountPlayersToTwo() {
    List<CoopResultBean> result = new ArrayList<>();
    result.add(CoopResultBeanBuilder.create().defaultValues()
        .replay(ReplayBeanBuilder.create().defaultValues()
            .teams(FXCollections.observableMap(Map.of("2", List.of("junit1"), "3", List.of("junit2"))))
            .get())
        .get());

    result.add(CoopResultBeanBuilder.create().defaultValues()
        .replay(ReplayBeanBuilder.create().defaultValues()
            .teams(FXCollections.observableMap(Map.of("2", List.of("test1"), "3", List.of("test2"))))
            .get())
        .get());

    result.add(CoopResultBeanBuilder.create().defaultValues()
        .replay(ReplayBeanBuilder.create().defaultValues()
            .teams(FXCollections.observableMap(Map.of("2", List.of("junit2"), "3", List.of("junit1"))))
            .get())
        .get());

    when(coopService.getLeaderboard(any(), eq(2))).thenReturn(completedFuture(result));

    instance.missionComboBox.getSelectionModel().select(new CoopMissionBean());

    runOnFxThreadAndWait(() -> {
      reinitialize(instance);
      instance.numberOfPlayersComboBox.getSelectionModel().select(2);
    });
    assertEquals(2, instance.leaderboardTable.getItems().size());
  }

  @Test
  public void testNoDuplicatedPlayersInTableWhenSetCountPlayersToAll() {
    List<CoopResultBean> result = new ArrayList<>();

    result.add(CoopResultBeanBuilder.create().defaultValues()
        .replay(ReplayBeanBuilder.create().defaultValues()
            .teams(FXCollections.observableMap(Map.of("2", List.of("junit1"), "3", List.of("junit2"))))
            .get())
        .get());

    result.add(CoopResultBeanBuilder.create().defaultValues()
        .replay(ReplayBeanBuilder.create().defaultValues()
            .teams(FXCollections.observableMap(Map.of("2", List.of("junit2"), "3", List.of("junit1"))))
            .get())
        .get());

    result.add(CoopResultBeanBuilder.create().defaultValues()
        .replay(ReplayBeanBuilder.create().defaultValues()
            .teams(FXCollections.observableMap(Map.of("2", List.of("junit1"))))
            .get())
        .get());

    result.add(CoopResultBeanBuilder.create().defaultValues()
        .replay(ReplayBeanBuilder.create().defaultValues()
            .teams(FXCollections.observableMap(Map.of("2", List.of("junit1"))))
            .get())
        .get());

    result.add(CoopResultBeanBuilder.create().defaultValues()
        .replay(ReplayBeanBuilder.create().defaultValues()
            .teams(FXCollections.observableMap(Map.of("2", List.of("test1", "test3"), "3", List.of("test2"))))
            .get())
        .get());

    when(coopService.getLeaderboard(any(), eq(0))).thenReturn(completedFuture(result));

    instance.missionComboBox.getSelectionModel().select(new CoopMissionBean());

    runOnFxThreadAndWait(() -> {
      reinitialize(instance);
      instance.numberOfPlayersComboBox.getSelectionModel().select(1);
      instance.numberOfPlayersComboBox.getSelectionModel().select(0);
    });
    assertEquals(3, instance.leaderboardTable.getItems().size());
  }
}
