package com.faforever.client.coop;

import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.game.FeaturedModBeanBuilder;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GamesTableController;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CompletableFuture;

import static com.faforever.client.game.KnownFeaturedMod.COOP;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoopControllerTest extends AbstractPlainJavaFxTest {
  @Rule
  public TemporaryFolder cacheDirectory = new TemporaryFolder();
  private CoopController instance;
  @Mock
  private CoopService coopService;
  @Mock
  private GameService gameService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private UiService uiService;
  @Mock
  private GamesTableController gamesTableController;
  @Mock
  private MapService mapService;
  @Mock
  private I18n i18n;
  @Mock
  private WebViewConfigurer webViewConfigurer;
  @Mock
  private ModService modService;

  private SimpleObjectProperty<Game> selectedGameProperty;

  @Mock
  private ReplayService replayService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private TimeService timeService;

  @Before
  public void setUp() throws Exception {
    instance = new CoopController(replayService, gameService, coopService, notificationService, i18n, reportingService,
        mapService, uiService, timeService, webViewConfigurer, modService);

    when(coopService.getLeaderboard(any(), anyInt())).thenReturn(CompletableFuture.completedFuture(emptyList()));
    when(coopService.getMissions()).thenReturn(CompletableFuture.completedFuture(emptyList()));
    when(modService.getFeaturedMod(COOP.getTechnicalName())).thenReturn(CompletableFuture.completedFuture(FeaturedModBeanBuilder.create().defaultValues().technicalName("coop").get()));
    when(gameService.getGames()).thenReturn(FXCollections.emptyObservableList());
    when(uiService.loadFxml("theme/play/games_table.fxml")).thenReturn(gamesTableController);
    when(gamesTableController.getRoot()).thenReturn(new Pane());

    loadFxml("theme/play/coop/coop.fxml", clazz -> instance);

    verify(webViewConfigurer).configureWebView(instance.descriptionWebView);
  }

  @Test
  public void onPlayButtonClicked() {
    when(coopService.getMissions()).thenReturn(completedFuture(singletonList(new CoopMission())));
    Platform.runLater(() -> instance.initialize());

    WaitForAsyncUtils.waitForFxEvents();
    instance.onPlayButtonClicked();

    ArgumentCaptor<NewGameInfo> captor = ArgumentCaptor.forClass(NewGameInfo.class);
    verify(gameService).hostGame(captor.capture());

    NewGameInfo newGameInfo = captor.getValue();
    assertThat(newGameInfo.getFeaturedMod().getTechnicalName(), is("coop"));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.coopRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }
}
