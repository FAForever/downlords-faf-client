package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

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
  @Mock
  private PreferencesService preferencesService;

  @InjectMocks
  private FriendOnlineNotifier instance;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);
  }

  @Test
  public void testNoToastOrSoundWhenDisabled() {
    preferencesService.getPreferences().getNotification().setFriendOnlineSoundEnabled(false);
    preferencesService.getPreferences().getNotification().setFriendOnlineToastEnabled(false);

    instance.onPlayerOnline(new PlayerOnlineEvent(PlayerBeanBuilder.create().defaultValues().username("axel12").get()));

    Mockito.verifyNoInteractions(notificationService, audioService);
  }

}
