package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameBuilder;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.event.FriendJoinedGameEvent;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.ReflectionUtils;

import static com.natpryce.hamcrest.reflection.HasAnnotationMatcher.hasAnnotation;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FriendJoinedGameNotifierTest {
  private FriendJoinedGameNotifier instance;
  @Mock
  private NotificationService notificationService;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;
  @Mock
  private JoinGameHelper joinGameHelper;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private AudioService audioService;

  private Preferences preferences;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new FriendJoinedGameNotifier(notificationService, i18n, eventBus, joinGameHelper, preferencesService, audioService);

    preferences = PreferencesBuilder.create().defaultValues().get();

    when(preferencesService.getPreferences()).thenReturn(preferences);

    instance.afterPropertiesSet();
    verify(eventBus).register(instance);
  }

  @Test
  public void testSubscribeAnnotations() {
    assertThat(ReflectionUtils.findMethod(instance.getClass(), "onFriendJoinedGame", FriendJoinedGameEvent.class),
        hasAnnotation(Subscribe.class));
  }

  @Test
  public void onFriendJoinedGame() throws Exception {
    Game game = GameBuilder.create().defaultValues().title("My Game").get();
    Player player = PlayerBuilder.create("junit").id(1).game(game).get();
    preferences.getNotification().setFriendJoinsGameToastEnabled(true);
    when(i18n.get("friend.joinedGameNotification.title", "junit", "My Game")).thenReturn("junit joined My Game");
    when(i18n.get("friend.joinedGameNotification.action")).thenReturn("Click to join");

    instance.onFriendJoinedGame(new FriendJoinedGameEvent(player, game));

    ArgumentCaptor<TransientNotification> captor = ArgumentCaptor.forClass(TransientNotification.class);
    verify(notificationService).addNotification(captor.capture());

    TransientNotification notification = captor.getValue();
    assertThat(notification.getTitle(), is("junit joined My Game"));
    assertThat(notification.getText(), is("Click to join"));
    assertThat(notification.getImage(), notNullValue());
  }

  @Test
  public void testNoNotificationIfDisabledInPreferences() throws Exception {
    preferences.getNotification().setFriendJoinsGameToastEnabled(false);

    instance.onFriendJoinedGame(new FriendJoinedGameEvent(PlayerBuilder.create("junit").get(), GameBuilder.create().get()));

    verify(notificationService, never()).addNotification(any(TransientNotification.class));
  }
}
