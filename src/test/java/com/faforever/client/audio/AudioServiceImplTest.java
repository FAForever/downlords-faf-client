package com.faforever.client.audio;

import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class AudioServiceImplTest extends AbstractPlainJavaFxTest {

  private AudioServiceImpl instance;
  private NotificationsPrefs notificationsPrefs;

  @Override
  public void start(Stage stage) throws Exception {
    instance = new AudioServiceImpl();
    instance.preferencesService = mock(PreferencesService.class);
    instance.audioClipPlayer = mock(AudioClipPlayer.class);
    instance.uiService = mock(UiService.class);

    Preferences preferences = new Preferences();
    notificationsPrefs = preferences.getNotification();

    when(instance.preferencesService.getPreferences()).thenReturn(preferences);
    when(instance.uiService.getThemeFileUrl(any())).thenReturn(getThemeFileUrl(UiService.MENTION_SOUND));

    instance.postConstruct();

    super.start(stage);
  }

  @Test
  public void testPlayChatMentionSound() throws Exception {
    notificationsPrefs.setSoundsEnabled(true);
    notificationsPrefs.setMentionSoundEnabled(true);

    instance.playChatMentionSound();

    verify(instance.audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayPrivateMessageSound() throws Exception {
    notificationsPrefs.setSoundsEnabled(true);
    notificationsPrefs.setPrivateMessageSoundEnabled(true);

    instance.playPrivateMessageSound();

    verify(instance.audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayInfoNotificationSound() throws Exception {
    notificationsPrefs.setSoundsEnabled(true);
    notificationsPrefs.setInfoSoundEnabled(true);

    instance.playInfoNotificationSound();

    verify(instance.audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayWarnNotificationSound() throws Exception {
    notificationsPrefs.setSoundsEnabled(true);
    notificationsPrefs.setWarnSoundEnabled(true);

    instance.playWarnNotificationSound();

    verify(instance.audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayErrorNotificationSound() throws Exception {
    notificationsPrefs.setSoundsEnabled(true);
    notificationsPrefs.setErrorSoundEnabled(true);

    instance.playErrorNotificationSound();

    verify(instance.audioClipPlayer).playSound(any(AudioClip.class));
  }
}
