package com.faforever.client.coop;

import com.faforever.client.domain.CoopMissionBean;
import com.faforever.client.featuredmod.FeaturedModService;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.game.GameRunner;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.game.GamesTableController;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import javafx.collections.FXCollections;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
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
  private FeaturedModService featuredModService;
  @Mock
  private ReplayService replayService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private TimeService timeService;
  @Mock
  private GameRunner gameRunner;

  @InjectMocks
  private CoopController instance;

  @BeforeEach
  public void setUp() throws Exception {
    lenient().when(coopService.getLeaderboard(any(), anyInt())).thenReturn(Flux.empty());
    lenient().when(coopService.getMissions()).thenReturn(Flux.empty());
    lenient().when(gameService.getGames()).thenReturn(FXCollections.emptyObservableList());
    lenient().when(uiService.loadFxml("theme/play/games_table.fxml")).thenReturn(gamesTableController);

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
    CoopMissionBean coopMission = Instancio.create(CoopMissionBean.class);
    when(coopService.getMissions()).thenReturn(Flux.just(coopMission));
    runOnFxThreadAndWait(() -> reinitialize(instance));

    instance.missionComboBox.getSelectionModel().select(coopMission);

    WaitForAsyncUtils.waitForFxEvents();
    instance.onPlayButtonClicked();

    ArgumentCaptor<NewGameInfo> captor = ArgumentCaptor.forClass(NewGameInfo.class);
    verify(gameRunner).host(captor.capture());

    NewGameInfo newGameInfo = captor.getValue();
    assertEquals("coop", newGameInfo.featuredModName());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.coopRoot, instance.getRoot());
    assertNull(instance.getRoot().getParent());
  }
}
