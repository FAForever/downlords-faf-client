package com.faforever.client.audio;

import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

  private Preferences preferences;

  @Override
  public void start(Stage stage) throws Exception {
    instance = new AudioService(preferencesService, audioClipPlayer, uiService);

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

    super.start(stage);
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
  public void testNoDoubleSound() throws Exception {
    instance.playChatMentionSound();
    instance.playChatMentionSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testSoundTurnedBackOn() throws Exception {
    instance.playChatMentionSound();

    WaitForAsyncUtils.sleep(35, TimeUnit.SECONDS);

    instance.playChatMentionSound();

    verify(audioClipPlayer, times(2)).playSound(any(AudioClip.class));
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
  public void testPlayFriendOfflineSound() throws Exception {
    instance.playFriendOfflineSound();

    verify(audioClipPlayer, never()).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayFriendOnlineSound() throws Exception {
    instance.playFriendOnlineSound();

    verify(audioClipPlayer, never()).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayFriendPlaysGameSound() throws Exception {
    instance.playFriendPlaysGameSound();

    verify(audioClipPlayer, never()).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayFriendJoinsGamSound() throws Exception {
    instance.playFriendJoinsGameSound();

    verify(audioClipPlayer, never()).playSound(any(AudioClip.class));
  }
}
