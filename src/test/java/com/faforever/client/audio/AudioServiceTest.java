package com.faforever.client.audio;

import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
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


public class AudioServiceTest extends AbstractPlainJavaFxTest {

  private AudioService instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private AudioClipPlayer audioClipPlayer;
  @Mock
  private UiService uiService;

  @Override
  public void start(Stage stage) throws Exception {
    instance = new AudioService(preferencesService, audioClipPlayer, uiService);

    Preferences preferences = PreferencesBuilder.create().defaultValues()
        .notificationsPrefs()
        .soundsEnabled(true)
        .privateMessageSoundEnabled(true)
        .mentionSoundEnabled(true)
        .infoSoundEnabled(true)
        .warnSoundEnabled(true)
        .errorSoundEnabled(true)
        .then()
        .get();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(uiService.getThemeFileUrl(any())).thenReturn(getThemeFileUrl(UiService.MENTION_SOUND));

    instance.afterPropertiesSet();

    super.start(stage);
  }

  @Test
  public void testPlayChatMentionSound() throws Exception {
    instance.playChatMentionSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayPrivateMessageSound() throws Exception {
    instance.playPrivateMessageSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayInfoNotificationSound() throws Exception {
    instance.playInfoNotificationSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayWarnNotificationSound() throws Exception {
    instance.playWarnNotificationSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayErrorNotificationSound() throws Exception {
    instance.playErrorNotificationSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }
}
