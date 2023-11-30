package com.faforever.client.audio;

import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.theme.ThemeService;
import javafx.scene.media.AudioClip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.core.io.ClassPathResource;
import org.testfx.util.WaitForAsyncUtils;

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
  private AudioClipPlayer audioClipPlayer;
  @Mock
  private ThemeService themeService;
  @Spy
  private NotificationPrefs notificationPrefs;

  @BeforeEach
  public void setUp() throws Exception {
    when(themeService.getThemeFileUrl(any())).thenReturn(
        new ClassPathResource(String.format("/%s", ThemeService.MENTION_SOUND)).getURL());

    notificationPrefs.setErrorSoundEnabled(true);
    notificationPrefs.setPrivateMessageSoundEnabled(true);
    notificationPrefs.setMentionSoundEnabled(true);
    notificationPrefs.setInfoSoundEnabled(true);
    notificationPrefs.setWarnSoundEnabled(true);
    notificationPrefs.setErrorSoundEnabled(true);
    notificationPrefs.setFriendJoinsGameSoundEnabled(true);
    notificationPrefs.setFriendOnlineSoundEnabled(true);
    notificationPrefs.setFriendPlaysGameSoundEnabled(true);
    notificationPrefs.setFriendOfflineSoundEnabled(true);

    instance.afterPropertiesSet();
  }

  @Test
  public void testNoSoundsWhenOff() {
    notificationPrefs.setSoundsEnabled(false);
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
    notificationPrefs.setFriendJoinsGameSoundEnabled(false);
    notificationPrefs.setErrorSoundEnabled(false);
    notificationPrefs.setInfoSoundEnabled(false);
    notificationPrefs.setWarnSoundEnabled(false);
    notificationPrefs.setMentionSoundEnabled(false);
    notificationPrefs.setFriendOfflineSoundEnabled(false);
    notificationPrefs.setFriendOnlineSoundEnabled(false);
    notificationPrefs.setFriendPlaysGameSoundEnabled(false);
    notificationPrefs.setPrivateMessageSoundEnabled(false);
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

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayFriendOnlineSound() {
    instance.playFriendOnlineSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayFriendPlaysGameSound() {
    instance.playFriendPlaysGameSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }

  @Test
  public void testPlayFriendJoinsGamSound() {
    instance.playFriendJoinsGameSound();

    verify(audioClipPlayer).playSound(any(AudioClip.class));
  }
}
