package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameBuilder;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.event.FollowedPlayerJoinedGameEvent;
import com.github.nocatch.NoCatch;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.ReflectionUtils;

import static com.natpryce.hamcrest.reflection.HasAnnotationMatcher.hasAnnotation;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class FollowedPlayerJoinedGameNotifierTest {
  private FollowedPlayerJoinedGameNotifier instance;
  @Mock
  private NotificationService notificationService;
  @Mock
  private EventBus eventBus;
  @Mock
  private I18n i18n;
  @Mock
  private JoinGameHelper joinGameHelper;
  @Mock
  private AudioService audioService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new FollowedPlayerJoinedGameNotifier(notificationService, i18n, eventBus, joinGameHelper, audioService);

    instance.postConstruct();
    verify(eventBus).register(instance);
  }

  @Test
  public void testSubscribeAnnotations() {
    assertThat(ReflectionUtils.findMethod(instance.getClass(), "onFollowedJoinedGame", FollowedPlayerJoinedGameEvent.class),
        hasAnnotation(Subscribe.class));
  }

  @Test
  public void onFollowedPlayerJoinedGame() throws Exception {
    Game game = GameBuilder.create().defaultValues().title("My Game").get();
    Player player = PlayerBuilder.create("junit").id(1).game(game).get();

    when(i18n.get("followed.joinedGameNotification.title", "junit", "My Game")).thenReturn("following junit");
    when(i18n.get("followed.joinedGameNotification.action")).thenReturn("Click to cancel");

    instance.onFollowedJoinedGame(new FollowedPlayerJoinedGameEvent(player, game));

    ArgumentCaptor<TransientNotification> captor = ArgumentCaptor.forClass(TransientNotification.class);
    verify(notificationService).addNotification(captor.capture());

    TransientNotification notification = captor.getValue();
    assertThat(notification.getTitle(), is("following junit"));
    assertThat(notification.getText(), is("Click to cancel"));
    assertThat(notification.getImage(), notNullValue());

    NoCatch.noCatch(() -> Thread.sleep(3500));//TODO shorten

    verify(joinGameHelper).join(game);
  }

  @Test
  public void onFollowedPlayerJoinedGameCANCELLED() throws Exception {
    Game game = GameBuilder.create().defaultValues().title("My Game").get();
    Player player = PlayerBuilder.create("junit").id(1).game(game).get();

    when(i18n.get("followed.joinedGameNotification.title", "junit", "My Game")).thenReturn("following junit");
    when(i18n.get("followed.joinedGameNotification.action")).thenReturn("Click to cancel");

    instance.onFollowedJoinedGame(new FollowedPlayerJoinedGameEvent(player, game));

    ArgumentCaptor<TransientNotification> captor = ArgumentCaptor.forClass(TransientNotification.class);
    verify(notificationService).addNotification(captor.capture());

    TransientNotification notification = captor.getValue();
    notification.getActionCallback().call(null);

    NoCatch.noCatch(() -> Thread.sleep(3500));//TODO shorten

    verifyZeroInteractions(joinGameHelper);
  }
}
