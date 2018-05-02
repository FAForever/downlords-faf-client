package com.faforever.client.audio;

import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class AudioServiceImplTest extends AbstractPlainJavaFxTest {

  private AudioServiceImpl instance;
  private NotificationsPrefs notificationsPrefs;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private AudioClipPlayer audioClipPlayer;
  @Mock
  private UiService uiService;

  @Override
  public void start(Stage stage) throws Exception {
    instance = new AudioServiceImpl(preferencesService, audioClipPlayer, uiService);

    Preferences preferences = new Preferences();
    notificationsPrefs = preferences.getNotification();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(uiService.getThemeFileUrl(any())).thenReturn(getThemeFileUrl(UiService.MENTION_SOUND));

    instance.postConstruct();

    super.start(stage);
  }

  @Test
  public void testPlayChatMentionSound() throws Exception {
    notificationsPrefs.setSoundsEnabled(true);
    notificationsPrefs.setMentionSoundEnabled(true);

    instance.playChatMentionSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayPrivateMessageSound() throws Exception {
    notificationsPrefs.setSoundsEnabled(true);
    notificationsPrefs.setPrivateMessageSoundEnabled(true);

    instance.playPrivateMessageSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayInfoNotificationSound() throws Exception {
    notificationsPrefs.setSoundsEnabled(true);
    notificationsPrefs.setInfoSoundEnabled(true);

    instance.playInfoNotificationSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayWarnNotificationSound() throws Exception {
    notificationsPrefs.setSoundsEnabled(true);
    notificationsPrefs.setWarnSoundEnabled(true);

    instance.playWarnNotificationSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayErrorNotificationSound() throws Exception {
    notificationsPrefs.setSoundsEnabled(true);
    notificationsPrefs.setErrorSoundEnabled(true);

    instance.playErrorNotificationSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }
}
