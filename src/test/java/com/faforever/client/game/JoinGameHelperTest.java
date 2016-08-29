package com.faforever.client.game;

import com.faforever.client.player.Player;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JoinGameHelperTest extends AbstractPlainJavaFxTest {

  @Mock
  ApplicationContext applicationContext;
  @Mock
  I18n i18n;
  @Mock
  PlayerService playerService;
  @Mock
  GameService gameService;
  @Mock
  MapService mapService;
  @Mock
  CreateGameController createGameController;
  @Mock
  EnterPasswordController enterPasswordController;
  @Mock
  PreferencesService preferencesService;
  @Mock
  NotificationService notificationService;
  @Mock
  ReportingService reportingService;
  @Mock
  Player player;
  @Mock
  Game game;
  @Mock
  ForgedAlliancePrefs forgedAlliancePrefs;
  private JoinGameHelper instance;
  @Mock
  private Path path;
  @Mock
  private Preferences preferences;

  @Before
  public void setUp() throws Exception {
    instance = new JoinGameHelper();
    instance.applicationContext = applicationContext;
    instance.i18n = i18n;
    instance.playerService = playerService;
    instance.gameService = gameService;
    instance.preferencesService = preferencesService;
    instance.enterPasswordController = enterPasswordController;
    instance.notificationService = notificationService;
    instance.setParentNode(this.getRoot());

    when(playerService.getCurrentPlayer()).thenReturn(player);
    when(player.getGlobalRatingMean()).thenReturn(1000.0f);
    when(player.getGlobalRatingDeviation()).thenReturn(0.0f);

    when(game.getMinRating()).thenReturn(0);
    when(game.getMaxRating()).thenReturn(1000);

    when(gameService.joinGame(any(), any())).thenReturn(new CompletableFuture<Void>());

    when(preferencesService.isGamePathValid()).thenReturn(true);

    instance.postConstruct();
  }


  /**
   * Ensure that a normal game is joined -> game path is set -> no password protection -> no rating notification
   */
  @Test
  public void testJoinGameSuccess() throws Exception {
    instance.join(game);
    verify(gameService).joinGame(game, null);
  }

  /**
   * Ensure that the user is allowed to choose the GameDirectory if no path is provided
   */
  @Test
  public void testJoinGameMissingGamePathUserSelectsValidPath() throws Exception {
    when(preferencesService.isGamePathValid()).thenReturn(false).thenReturn(true);
    when(preferencesService.letUserChooseGameDirectory()).thenReturn(CompletableFuture.completedFuture(Paths.get("")));

    instance.join(game);

    verify(preferencesService, times(1)).letUserChooseGameDirectory();
    verify(gameService).joinGame(any(), any());
  }

  /**
   * Ensure that the user is allowed to choose the GameDirectory if no path is provided
   */
  @Test
  public void testJoinGameMissingGamePathUserSelectsInvalidPath() throws Exception {
    when(preferencesService.isGamePathValid()).thenReturn(false);

    // First, user selects invalid path. Seconds, he aborts so we don't stay in an endless loop
    when(preferencesService.letUserChooseGameDirectory())
        .thenReturn(CompletableFuture.completedFuture(Paths.get("")))
        .thenReturn(CompletableFuture.completedFuture(null));

    instance.join(game);

    verify(preferencesService, times(2)).letUserChooseGameDirectory();
    verify(gameService, never()).joinGame(any(), any());
  }

  /**
   * Ensure that the user is asked for password using enterPasswordController
   */
  @Test
  public void testJoinGamePasswordProtected() throws Exception {
    when(game.getPasswordProtected()).thenReturn(true);
    instance.join(game);
    verify(instance.enterPasswordController).showPasswordDialog(getRoot().getScene().getWindow());
  }

  /**
   * Ensure that the user is _not_ notified about his rating if ignoreRating is true
   */
  @Test
  public void testJoinGameIgnoreRatings() throws Exception {
    when(game.getMinRating()).thenReturn(5000);
    when(game.getMaxRating()).thenReturn(100);
    instance.join(game, "haha", true);
    verify(gameService).joinGame(game, "haha");
    verify(notificationService, never()).addNotification(any(ImmediateNotification.class));
  }

  /**
   * Ensure that the user is notified about his rating being to low
   */
  @Test
  public void testJoinGameRatingToLow() throws Exception {
    when(game.getMinRating()).thenReturn(5000);
    instance.join(game);
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  /**
   * Ensure that the user is notified about his rating being to high
   */
  @Test
  public void testJoinGameRatingToHigh() throws Exception {
    when(game.getMaxRating()).thenReturn(100);
    instance.join(game);
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }
}
