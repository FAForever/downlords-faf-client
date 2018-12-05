package com.faforever.client.audio;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import javafx.scene.media.AudioClip;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.InitializingBean;

import javax.inject.Inject;
import java.io.IOException;

@Lazy
@Service
public class AudioService implements InitializingBean {

  private static final String ACHIEVEMENT_UNLOCKED_SOUND = "theme/sounds/achievement_unlocked.mp3";
  private static final String INFO_SOUND = "theme/sounds/info.mp3";
  private static final String MENTION_SOUND = "theme/sounds/mention.mp3";
  private static final String PRIVATE_MESSAGE_SOUND = "theme/sounds/pm.mp3";

  private final PreferencesService preferencesService;
  private final AudioClipPlayer audioClipPlayer;
  private final UiService uiService;

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

  private boolean playSounds;
  private NotificationsPrefs notificationsPrefs;

  @Inject
  public AudioService(PreferencesService preferencesService, AudioClipPlayer audioClipPlayer, UiService uiService) {
    this.preferencesService = preferencesService;
    this.audioClipPlayer = audioClipPlayer;
    this.uiService = uiService;
  }

  @Override
  public void afterPropertiesSet() throws IOException {
    notificationsPrefs = preferencesService.getPreferences().getNotification();
    JavaFxUtil.addListener(notificationsPrefs.soundsEnabledProperty(), (observable, oldValue, newValue) ->
        playSounds = newValue
    );
    playSounds = notificationsPrefs.isSoundsEnabled();

    loadSounds();
  }

  private void loadSounds() throws IOException {
    achievementUnlockedSound = loadSound(ACHIEVEMENT_UNLOCKED_SOUND);
    infoNotificationSound = loadSound(INFO_SOUND);
    errorNotificationSound = loadSound(INFO_SOUND);
    warnNotificationSound = loadSound(INFO_SOUND);
    chatMentionSound = loadSound(MENTION_SOUND);
    privateMessageSound = loadSound(PRIVATE_MESSAGE_SOUND);
    // TODO implement
//    friendOnlineSound = loadSound(FRIEND_ONLINE_SOUND);
//    friendOfflineSound = loadSound(FRIEND_OFFLINE_SOUND);
//    friendJoinsGameSound = loadSound(FRIEND_JOINS_GAME_SOUND);
//    friendPlaysGameSound = loadSound(FRIEND_PLAYS_GAME_SOUND);
  }

  private AudioClip loadSound(String sound) throws IOException {
    return new AudioClip(uiService.getThemeFileUrl(sound).toString());
  }

  
  public void playChatMentionSound() {
    if (!notificationsPrefs.isMentionSoundEnabled()) {
      return;
    }
    playSound(chatMentionSound);
  }

  
  public void playPrivateMessageSound() {
    if (!notificationsPrefs.isPrivateMessageSoundEnabled()) {
      return;
    }
    playSound(privateMessageSound);
  }

  
  public void playInfoNotificationSound() {
    if (!notificationsPrefs.isInfoSoundEnabled()) {
      return;
    }
    playSound(infoNotificationSound);
  }

  
  public void playWarnNotificationSound() {
    if (!notificationsPrefs.isWarnSoundEnabled()) {
      return;
    }
    playSound(warnNotificationSound);
  }

  
  public void playErrorNotificationSound() {
    if (!notificationsPrefs.isErrorSoundEnabled()) {
      return;
    }
    playSound(errorNotificationSound);
  }

  
  public void playAchievementUnlockedSound() {
    playSound(achievementUnlockedSound);
  }

  
  public void playFriendOnlineSound() {
    if (!notificationsPrefs.isFriendOnlineSoundEnabled()) {
      return;
    }
    // FIXME implement
//    playSound(friendOnlineSound);
  }

  
  public void playFriendOfflineSound() {
    if (!notificationsPrefs.isFriendOfflineSoundEnabled()) {
      return;
    }
    // FIXME implement
//    playSound(friendOfflineSound);
  }

  
  public void playFriendJoinsGameSound() {
    if (!notificationsPrefs.isFriendJoinsGameSoundEnabled()) {
      return;
    }
    // FIXME implement
//    playSound(friendJoinsGameSound);
  }

  
  public void playFriendPlaysGameSound() {
    if (!notificationsPrefs.isFriendPlaysGameSoundEnabled()) {
      return;
    }
    playSound(friendPlaysGameSound);
  }

  private void playSound(AudioClip audioClip) {
    if (!playSounds) {
      return;
    }
    audioClipPlayer.playSound(audioClip);
  }
}
