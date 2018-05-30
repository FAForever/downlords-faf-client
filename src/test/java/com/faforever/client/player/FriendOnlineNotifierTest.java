package com.faforever.client.player;

import com.faforever.client.audio.AudioService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class FriendOnlineNotifierTest {
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

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    preferencesService = new PreferencesService();
    preferencesService.postConstruct();

    instance = new FriendOnlineNotifier(notificationService, i18n, eventBus, audioService, playerService, preferencesService);
  }

  @Test
  public void testNoToastOrSoundWhenDisabled() {
    preferencesService.getPreferences().getNotification().setFriendOnlineSoundEnabled(false);
    preferencesService.getPreferences().getNotification().setFriendOnlineToastEnabled(false);

    instance.onUserOnline(new PlayerOnlineEvent(new Player("axel12")));

    Mockito.verifyZeroInteractions(notificationService, audioService);
  }

}
