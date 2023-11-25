package com.faforever.client.audio;

import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.theme.ThemeService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.media.AudioClip;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Lazy
@Service
@RequiredArgsConstructor
public class AudioService implements InitializingBean {

  private static final String ACHIEVEMENT_UNLOCKED_SOUND = "theme/sounds/achievementUnlockedSound.mp3";
  private static final String NOTIFICATION_INFO_SOUND = "theme/sounds/infoNotificationSound.mp3";
  private static final String NOTIFICATION_WARN_SOUND = "theme/sounds/warnNotificationSound.mp3";
  private static final String NOTIFICATION_ERROR_SOUND = "theme/sounds/errorNotificationSound.mp3";
  private static final String MENTION_SOUND = "theme/sounds/userMentionSound.mp3";
  private static final String PRIVATE_MESSAGE_SOUND = "theme/sounds/privateMessageSound.mp3";
  private static final String FRIEND_ONLINE_SOUND = "theme/sounds/friendOnlineSound.mp3";
  private static final String FRIEND_OFFLINE_SOUND = "theme/sounds/friendOfflineSound.mp3";
  private static final String FRIEND_JOINS_GAME_SOUND = "theme/sounds/friendJoinsGameSound.mp3";
  private static final String FRIEND_PLAYS_GAME_SOUND = "theme/sounds/friendPlaysGameSound.mp3";
  private static final long MILLISECONDS_SILENT_AFTER_SOUND = 30000;

  private final AudioClipPlayer audioClipPlayer;
  private final ThemeService themeService;
  private final NotificationPrefs notificationPrefs;

  private final BooleanProperty playSounds = new SimpleBooleanProperty();

  private AudioClip chatMentionSound;
  private AudioClip achievementUnlockedSound;
  private AudioClip errorNotificationSound;
  private AudioClip infoNotificationSound;
  private AudioClip warnNotificationSound;
  private AudioClip privateMessageSound;
  private AudioClip friendOnlineSound;
  private AudioClip friendOfflineSound;
  private AudioClip friendJoinsGameSound;
  private AudioClip friendPlaysGameSound;

  private long lastPlayedSoundTime;

  @Override
  public void afterPropertiesSet() throws IOException {
    playSounds.bind(notificationPrefs.soundsEnabledProperty());

    loadSounds();
  }

  private void loadSounds() throws IOException {
    achievementUnlockedSound = loadSound(ACHIEVEMENT_UNLOCKED_SOUND);
    infoNotificationSound = loadSound(NOTIFICATION_INFO_SOUND);
    errorNotificationSound = loadSound(NOTIFICATION_ERROR_SOUND);
    warnNotificationSound = loadSound(NOTIFICATION_WARN_SOUND);
    chatMentionSound = loadSound(MENTION_SOUND);
    privateMessageSound = loadSound(PRIVATE_MESSAGE_SOUND);
    friendOnlineSound = loadSound(FRIEND_ONLINE_SOUND);
    friendOfflineSound = loadSound(FRIEND_OFFLINE_SOUND);
    friendJoinsGameSound = loadSound(FRIEND_JOINS_GAME_SOUND);
    friendPlaysGameSound = loadSound(FRIEND_PLAYS_GAME_SOUND);
  }

  private AudioClip loadSound(String sound) throws IOException {
    return new AudioClip(themeService.getThemeFileUrl(sound).toString());
  }


  public void playChatMentionSound() {
    if (!notificationPrefs.isMentionSoundEnabled()) {
      return;
    }
    playSound(chatMentionSound);
  }


  public void playPrivateMessageSound() {
    if (!notificationPrefs.isPrivateMessageSoundEnabled()) {
      return;
    }
    playSound(privateMessageSound);
  }


  public void playInfoNotificationSound() {
    if (!notificationPrefs.isInfoSoundEnabled()) {
      return;
    }
    playSound(infoNotificationSound);
  }


  public void playWarnNotificationSound() {
    if (!notificationPrefs.isWarnSoundEnabled()) {
      return;
    }
    playSound(warnNotificationSound);
  }


  public void playErrorNotificationSound() {
    if (!notificationPrefs.isErrorSoundEnabled()) {
      return;
    }
    playSound(errorNotificationSound);
  }


  public void playAchievementUnlockedSound() {
    playSound(achievementUnlockedSound);
  }


  public void playFriendOnlineSound() {
    if (!notificationPrefs.isFriendOnlineSoundEnabled()) {
      return;
    }
    playSound(friendOnlineSound);
  }


  public void playFriendOfflineSound() {
    if (!notificationPrefs.isFriendOfflineSoundEnabled()) {
      return;
    }
    playSound(friendOfflineSound);
  }


  public void playFriendJoinsGameSound() {
    if (!notificationPrefs.isFriendJoinsGameSoundEnabled()) {
      return;
    }
    playSound(friendJoinsGameSound);
  }


  public void playFriendPlaysGameSound() {
    if (!notificationPrefs.isFriendPlaysGameSoundEnabled()) {
      return;
    }
    playSound(friendPlaysGameSound);
  }

  private void playSound(AudioClip audioClip) {
    if (!playSounds.get()) {
      return;
    }

    long currentTimeMillis = System.currentTimeMillis();
    if (currentTimeMillis - lastPlayedSoundTime < MILLISECONDS_SILENT_AFTER_SOUND) {
      return;
    }
    lastPlayedSoundTime = currentTimeMillis;
    audioClipPlayer.playSound(audioClip);
  }
}
