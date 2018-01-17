package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.notificationEvents.ShowImmediateNotificationEvent;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.SupportService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
  private ApplicationEventPublisher applicationEventPublisher;
  @Mock
  private SupportService supportService;
  @Mock
  private Player player;
  @Mock
  private Game game;
  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;

  @Before
  public void setUp() throws Exception {
    instance = new JoinGameHelper(i18n, playerService, gameService, preferencesService, applicationEventPublisher, supportService, uiService, eventBus);

    when(playerService.getCurrentPlayer()).thenReturn(Optional.ofNullable(player));
    when(player.getGlobalRatingMean()).thenReturn(1000.0f);
    when(player.getGlobalRatingDeviation()).thenReturn(0.0f);

    when(game.getMinRating()).thenReturn(0);
    when(game.getMaxRating()).thenReturn(1000);
    when(uiService.loadFxml("theme/enter_password.fxml")).thenReturn(enterPasswordController);

    when(gameService.joinGame(any(), any())).thenReturn(new CompletableFuture<>());

    when(preferencesService.isGamePathValid()).thenReturn(true);
  }


  /**
   * Ensure that a normal preferences is joined -> preferences path is set -> no password protection -> no rating notification
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
  public void testJoinGameMissingGamePathUserSelectsInvalidPath() throws Exception {
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
  public void testJoinGamePasswordProtected() throws Exception {
    when(game.getPasswordProtected()).thenReturn(true);
    instance.join(game);
    verify(enterPasswordController).showPasswordDialog(getRoot().getScene().getWindow());
  }

  /**
   * Ensure that the user is _not_ notified about his rating if ignoreRating is true
   */
  @Test
  public void testJoinGameIgnoreRatings() throws Exception {
    instance.join(game, "haha", true);
    verify(gameService).joinGame(game, "haha");
    verify(applicationEventPublisher, never()).publishEvent(any(ShowImmediateNotificationEvent.class));

    verify(game, never()).getMinRating();
    verify(game, never()).getMaxRating();
  }

  /**
   * Ensure that the user is notified about his rating being to low
   */
  @Test
  public void testJoinGameRatingToLow() throws Exception {
    when(game.getMinRating()).thenReturn(5000);
    instance.join(game);
    verify(applicationEventPublisher).publishEvent(any(ShowImmediateNotificationEvent.class));
  }

  /**
   * Ensure that the user is notified about his rating being to high
   */
  @Test
  public void testJoinGameRatingToHigh() throws Exception {
    when(game.getMaxRating()).thenReturn(100);
    instance.join(game);
    verify(applicationEventPublisher).publishEvent(any(ShowImmediateNotificationEvent.class));
  }
}
