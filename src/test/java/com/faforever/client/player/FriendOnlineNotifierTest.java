package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class FriendOnlineNotifierTest extends ServiceTest {
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private EventBus eventBus;
  @Mock
  private AudioService audioService;
  @Mock
  private PlayerService playerService;

  private PreferencesService preferencesService;

  private FriendOnlineNotifier instance;

  @BeforeEach
  public void setUp() throws Exception {
    preferencesService = new PreferencesService(new ClientProperties());
    preferencesService.afterPropertiesSet();

    instance = new FriendOnlineNotifier(notificationService, i18n, eventBus, audioService, playerService, preferencesService);
  }

  @Test
  public void testNoToastOrSoundWhenDisabled() {
    preferencesService.getPreferences().getNotification().setFriendOnlineSoundEnabled(false);
    preferencesService.getPreferences().getNotification().setFriendOnlineToastEnabled(false);

    instance.onPlayerOnline(new PlayerOnlineEvent(PlayerBeanBuilder.create().defaultValues().username("axel12").get()));

    Mockito.verifyNoInteractions(notificationService, audioService);
  }

}
