package com.faforever.client.audio;

import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.theme.UiService;
import javafx.scene.media.AudioClip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.io.ClassPathResource;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class AudioServiceTest extends ServiceTest {

  @InjectMocks
  private AudioService instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private AudioClipPlayer audioClipPlayer;
  @Mock
  private UiService uiService;

  private Preferences preferences;

  private String getThemeFile(String file) {
    return String.format("/%s", file);
  }

  private URL getThemeFileUrl(String file) throws IOException {
    return new ClassPathResource(getThemeFile(file)).getURL();
  }

  @BeforeEach
  public void setUp() throws Exception {
    preferences = PreferencesBuilder.create().defaultValues()
        .notificationsPrefs()
        .soundsEnabled(true)
        .privateMessageSoundEnabled(true)
        .mentionSoundEnabled(true)
        .infoSoundEnabled(true)
        .warnSoundEnabled(true)
        .errorSoundEnabled(true)
        .friendJoinsGameSoundEnabled(true)
        .friendOnlineSoundEnabled(true)
        .friendPlaysGameSoundEnabled(true)
        .friendOfflineSoundEnabled(true)
        .then()
        .get();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(uiService.getThemeFileUrl(any())).thenReturn(getThemeFileUrl(UiService.MENTION_SOUND));

    instance.afterPropertiesSet();
  }

  @Test
  public void testNoSoundsWhenOff() {
    NotificationsPrefs notificationsPrefs = preferences.getNotification();
    notificationsPrefs.setSoundsEnabled(false);
    instance.playChatMentionSound();
    instance.playPrivateMessageSound();
    instance.playInfoNotificationSound();
    instance.playAchievementUnlockedSound();
    instance.playErrorNotificationSound();
    instance.playWarnNotificationSound();
    instance.playFriendJoinsGameSound();
    instance.playFriendOnlineSound();
    instance.playFriendOfflineSound();
    instance.playFriendPlaysGameSound();
    instance.playInfoNotificationSound();
    verify(audioClipPlayer, never()).playSound(any());
  }

  @Test
  public void testNoSoundsWhenIndividuallyOff() {
    NotificationsPrefs notificationsPrefs = preferences.getNotification();
    notificationsPrefs.setFriendJoinsGameSoundEnabled(false);
    notificationsPrefs.setErrorSoundEnabled(false);
    notificationsPrefs.setInfoSoundEnabled(false);
    notificationsPrefs.setWarnSoundEnabled(false);
    notificationsPrefs.setMentionSoundEnabled(false);
    notificationsPrefs.setFriendOfflineSoundEnabled(false);
    notificationsPrefs.setFriendOnlineSoundEnabled(false);
    notificationsPrefs.setFriendPlaysGameSoundEnabled(false);
    notificationsPrefs.setPrivateMessageSoundEnabled(false);
    instance.playChatMentionSound();
    instance.playPrivateMessageSound();
    instance.playInfoNotificationSound();
    instance.playErrorNotificationSound();
    instance.playWarnNotificationSound();
    instance.playFriendJoinsGameSound();
    instance.playFriendOnlineSound();
    instance.playFriendOfflineSound();
    instance.playFriendPlaysGameSound();
    instance.playInfoNotificationSound();
    verify(audioClipPlayer, never()).playSound(any());
  }

  @Test
  public void testNoDoubleSound() {
    instance.playChatMentionSound();
    instance.playChatMentionSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testSoundTurnedBackOn() {
    instance.playChatMentionSound();

    WaitForAsyncUtils.sleep(35, TimeUnit.SECONDS);

    instance.playChatMentionSound();

    verify(audioClipPlayer, times(2)).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayChatMentionSound() {
    instance.playChatMentionSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayPrivateMessageSound() {
    instance.playPrivateMessageSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayInfoNotificationSound() {
    instance.playInfoNotificationSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayWarnNotificationSound() {
    instance.playWarnNotificationSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayFriendOfflineSound() {
    instance.playFriendOfflineSound();

    verify(audioClipPlayer, never()).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayFriendOnlineSound() {
    instance.playFriendOnlineSound();

    verify(audioClipPlayer, never()).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayFriendPlaysGameSound() {
    instance.playFriendPlaysGameSound();

    verify(audioClipPlayer, never()).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayFriendJoinsGamSound() {
    instance.playFriendJoinsGameSound();

    verify(audioClipPlayer, never()).playSound(any(AudioClip.class));
  }
}
