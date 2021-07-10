package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class JoinGameHelperTest extends AbstractPlainJavaFxTest {

  private JoinGameHelper instance;

  @Mock
  private I18n i18n;
  @Mock
  private PlayerService playerService;
  @Mock
  private GameService gameService;
  @Mock
  private EnterPasswordController enterPasswordController;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;

  private Game game;

  @BeforeEach
  public void setUp() throws Exception {
    Player currentPlayer = PlayerBuilder.create("junit").defaultValues().get();
    game = GameBuilder.create().defaultValues()
        .minRating(0)
        .maxRating(1000)
        .get();

    instance = new JoinGameHelper(i18n, playerService, gameService, preferencesService, notificationService, reportingService, uiService, eventBus);

    when(playerService.getCurrentPlayer()).thenReturn(currentPlayer);

    when(uiService.loadFxml("theme/enter_password.fxml")).thenReturn(enterPasswordController);

    when(gameService.joinGame(any(), any())).thenReturn(new CompletableFuture<>());

    when(preferencesService.isGamePathValid()).thenReturn(true);
  }


  /**
   * Ensure that a normal preferences is joined -> preferences path is set -> no password protection -> no rating
   * notification
   */
  @Test
  public void testJoinGameSuccess() {
    instance.join(game);
    verify(gameService).joinGame(game, null);
  }

  /**
   * Ensure that the user is allowed to choose the GameDirectory if no path is provided
   */
  @Test
  public void testJoinGameMissingGamePathUserSelectsValidPath() {
    when(preferencesService.isGamePathValid()).thenReturn(false).thenReturn(true);

    doAnswer(invocation -> {
      ((GameDirectoryChooseEvent) invocation.getArgument(0)).getFuture().ifPresent(future -> future.complete(Paths.get("")));
      return null;
    }).when(eventBus).post(any(GameDirectoryChooseEvent.class));

    instance.join(game);

    verify(eventBus, times(1)).post(Mockito.any(GameDirectoryChooseEvent.class));
    verify(gameService).joinGame(any(), any());
  }

  /**
   * Ensure that the user is allowed to choose the GameDirectory if no path is provided
   */
  @Test
  public void testJoinGameMissingGamePathUserSelectsInvalidPath() {
    when(preferencesService.isGamePathValid()).thenReturn(false);

    // First, user selects invalid path. Seconds, he aborts so we don't stay in an endless loop
    AtomicInteger invocationCounter = new AtomicInteger();
    doAnswer(invocation -> {
      Optional<CompletableFuture<Path>> optional = ((GameDirectoryChooseEvent) invocation.getArgument(0)).getFuture();
      if (invocationCounter.incrementAndGet() == 1) {
        optional.ifPresent(future -> future.complete(Paths.get("")));
      } else {
        optional.ifPresent(future -> future.complete(null));
      }
      return null;
    }).when(eventBus).post(any(GameDirectoryChooseEvent.class));

    instance.join(game);

    verify(eventBus, times(2)).post(Mockito.any(GameDirectoryChooseEvent.class));
    verify(gameService, never()).joinGame(any(), any());
  }

  /**
   * Ensure that the user is asked for password using enterPasswordController
   */
  @Test
  public void testJoinGamePasswordProtected() {
    game.setPasswordProtected(true);
    instance.join(game);
    verify(enterPasswordController).showPasswordDialog(getRoot().getScene().getWindow());
  }

  /**
   * Ensure that the user is _not_ notified about his rating if ignoreRating is true
   */
  @Test
  public void testJoinGameIgnoreRatings() {
    game.setMaxRating(-100);
    instance.join(game, "haha", true);
    verify(gameService).joinGame(game, "haha");
    verify(notificationService, never()).addNotification(any(ImmediateNotification.class));
    verifyNoInteractions(notificationService);
  }

  /**
   * Ensure that the user is notified about his rating being to low
   */
  @Test
  public void testJoinGameRatingToLow() {
    game.setMinRating(5000);
    instance.join(game);
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  /**
   * Ensure that the user is notified about his rating being to high
   */
  @Test
  public void testJoinGameRatingToHigh() {
    game.setMaxRating(-100);
    instance.join(game);
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }
}
