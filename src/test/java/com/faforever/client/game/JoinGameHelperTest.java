package com.faforever.client.game;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.preferences.GameDirectoryRequiredHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class JoinGameHelperTest extends PlatformTest {

  @InjectMocks
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
  private GameDirectoryRequiredHandler gameDirectoryRequiredHandler;

  private GameBean game;

  @BeforeEach
  public void setUp() throws Exception {
    PlayerBean currentPlayer = PlayerBeanBuilder.create().defaultValues().get();
    game = GameBeanBuilder.create().defaultValues()
        .ratingMin(0)
        .ratingMax(1000)
        .get();

    when(playerService.getCurrentPlayer()).thenReturn(currentPlayer);

    when(uiService.loadFxml("theme/enter_password.fxml")).thenReturn(enterPasswordController);

    when(gameService.joinGame(any(), any())).thenReturn(new CompletableFuture<>());

    when(preferencesService.isValidGamePath()).thenReturn(true);
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
    when(preferencesService.isValidGamePath()).thenReturn(false).thenReturn(true);

    doAnswer(invocation -> {
      CompletableFuture<Path> argument = invocation.getArgument(0);
      argument.complete(Path.of(""));
      return null;
    }).when(gameDirectoryRequiredHandler).onChooseGameDirectory(any(CompletableFuture.class));

    instance.join(game);

    verify(gameDirectoryRequiredHandler).onChooseGameDirectory(any());
    verify(gameService).joinGame(any(), any());
  }

  /**
   * Ensure that the user is allowed to choose the GameDirectory if no path is provided
   */
  @Test
  public void testJoinGameMissingGamePathUserSelectsInvalidPath() {
    when(preferencesService.isValidGamePath()).thenReturn(false);

    // First, user selects invalid path. Seconds, he aborts so we don't stay in an endless loop
    AtomicInteger invocationCounter = new AtomicInteger();
    doAnswer(invocation -> {
      CompletableFuture<Path> future = invocation.getArgument(0);
      if (invocationCounter.incrementAndGet() == 1) {
        future.complete(Path.of(""));
      } else {
        future.complete(null);
      }
      return null;
    }).when(gameDirectoryRequiredHandler).onChooseGameDirectory(any(CompletableFuture.class));

    instance.join(game);

    verify(gameDirectoryRequiredHandler, times(2)).onChooseGameDirectory(any());
    verify(gameService, never()).joinGame(any(), any());
  }

  /**
   * Ensure that the user is _not_ notified about his rating if ignoreRating is true
   */
  @Test
  public void testJoinGameIgnoreRatings() {
    game.setRatingMax(-100);
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
    game.setRatingMin(5000);
    instance.join(game);
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  /**
   * Ensure that the user is notified about his rating being to high
   */
  @Test
  public void testJoinGameRatingToHigh() {
    game.setRatingMax(-100);
    instance.join(game);
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }
}
