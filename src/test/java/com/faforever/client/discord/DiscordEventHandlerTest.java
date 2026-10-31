package com.faforever.client.discord;

import com.faforever.client.domain.server.GameInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.game.GameRunner;
import com.faforever.client.game.GameService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.replay.ReplayRunner;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.user.LoginService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DiscordEventHandlerTest extends ServiceTest {

  private static final String JOIN_SECRET = "{\"gameId\":5}";

  @Mock
  private NotificationService notificationService;
  @Mock
  private GameRunner gameRunner;
  @Mock
  private ReplayRunner replayRunner;
  @Mock
  private GameService gameService;
  @Mock
  private LoginService loginService;
  @Mock
  private FxApplicationThreadExecutor fxApplicationThreadExecutor;
  @Mock
  private TaskScheduler taskScheduler;

  private final Preferences preferences = new Preferences();
  private final SimpleObjectProperty<GameInfo> game = new SimpleObjectProperty<>();
  private final SimpleObjectProperty<ConnectionState> connectionState =
      new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);
  private final GameInfo gameInfo = new GameInfo();

  private DiscordEventHandler instance;

  @BeforeEach
  public void setUp() {
    lenient().doAnswer(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return null;
    }).when(fxApplicationThreadExecutor).execute(any());
    lenient().when(gameService.observeByUid(5)).thenReturn(game);
    lenient().when(loginService.connectionStateProperty()).thenReturn(connectionState);
    lenient().when(loginService.getConnectionState()).thenAnswer(invocation -> connectionState.get());

    instance = new DiscordEventHandler(notificationService, gameRunner, replayRunner, gameService, new ObjectMapper(),
                                       preferences, loginService, fxApplicationThreadExecutor, taskScheduler);
  }

  @Test
  public void joinKnownGame() {
    game.set(gameInfo);

    instance.onJoin(JOIN_SECRET);

    verify(gameRunner).join(gameInfo);
  }

  @Test
  public void spectateKnownGame() {
    game.set(gameInfo);

    instance.onSpectate(JOIN_SECRET);

    verify(replayRunner).runWithLiveReplay(gameInfo);
  }

  @Test
  public void joinIsDeferredUntilGameIsKnown() {
    instance.onJoin(JOIN_SECRET);
    verify(gameRunner, never()).join(any());

    connectionState.set(ConnectionState.CONNECTED);
    game.set(gameInfo);

    verify(gameRunner).join(gameInfo);
    Runnable timeout = captureTimeout();
    timeout.run();
    verifyNoInteractions(notificationService);
  }

  @Test
  public void joinFailsIfGameDoesNotAppearAfterConnecting() {
    instance.onJoin(JOIN_SECRET);
    verifyNoInteractions(taskScheduler);

    connectionState.set(ConnectionState.CONNECTED);
    captureTimeout().run();
    game.set(gameInfo);

    verify(gameRunner, never()).join(any());
    verify(notificationService).addImmediateErrorNotification(any(IllegalStateException.class),
                                                              eq("discord.couldNotOpen"));
  }

  @Test
  public void joinIsRejectedIfDisallowed() {
    preferences.setDisallowJoinsViaDiscord(true);
    game.set(gameInfo);

    instance.onJoin(JOIN_SECRET);

    verifyNoInteractions(gameRunner);
  }

  private Runnable captureTimeout() {
    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(taskScheduler).schedule(captor.capture(), any(Instant.class));
    return captor.getValue();
  }
}
