package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FriendJoinedGameNotifierTest extends ServiceTest {
  @InjectMocks
  private FriendJoinedGameNotifier instance;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private JoinGameHelper joinGameHelper;

  @Mock
  private AudioService audioService;
  @Spy
  private NotificationPrefs notificationPrefs;

  @Test
  public void onFriendJoinedGame() throws Exception {
    GameBean game = GameBeanBuilder.create().defaultValues().title("My Game").get();
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().username("junit").id(1).game(game).get();
    notificationPrefs.setFriendJoinsGameToastEnabled(true);
    when(i18n.get("friend.joinedGameNotification.title", "junit", "My Game")).thenReturn("junit joined My Game");
    when(i18n.get("friend.joinedGameNotification.action")).thenReturn("Click to join");

    instance.onFriendJoinedGame(player, game);

    ArgumentCaptor<TransientNotification> captor = ArgumentCaptor.forClass(TransientNotification.class);
    verify(notificationService).addNotification(captor.capture());

    TransientNotification notification = captor.getValue();
    assertThat(notification.getTitle(), is("junit joined My Game"));
    assertThat(notification.getText(), is("Click to join"));
    assertThat(notification.getImage(), notNullValue());
  }

  @Test
  public void testNoNotificationIfDisabledInPreferences() throws Exception {
    notificationPrefs.setFriendJoinsGameToastEnabled(false);

    instance.onFriendJoinedGame(PlayerBeanBuilder.create().defaultValues().username("junit").get(),
                                GameBeanBuilder.create().defaultValues().get());

    verify(notificationService, never()).addNotification(any(TransientNotification.class));
  }
}
