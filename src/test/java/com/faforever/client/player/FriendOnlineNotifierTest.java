package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

public class FriendOnlineNotifierTest extends ServiceTest {
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private AudioService audioService;
  @Mock
  private PlayerService playerService;

  @Spy
  private NotificationPrefs notificationPrefs;

  @InjectMocks
  private FriendOnlineNotifier instance;

  @Test
  public void testNoToastOrSoundWhenDisabled() {
    notificationPrefs.setFriendOnlineSoundEnabled(false);
    notificationPrefs.setFriendOnlineToastEnabled(false);

    instance.onPlayerOnline(PlayerBeanBuilder.create().defaultValues().username("axel12").get());

    Mockito.verifyNoInteractions(notificationService, audioService);
  }

}
