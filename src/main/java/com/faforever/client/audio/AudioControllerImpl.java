package com.faforever.client.audio;

import com.faforever.client.main.MainController;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.ThemeService;
import javafx.scene.media.AudioClip;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;

public class AudioControllerImpl implements AudioController {

  private static final String ACHIEVEMENT_UNLOCKED_SOUND = "sounds/achievement_unlocked.mp3";
  private static final String INFO_SOUND = "sounds/info.mp3";
  private static final String MENTION_SOUND = "sounds/mention.mp3";
  private static final String PRIVATE_MESSAGE_SOUND = "sounds/pm.mp3";

  @Resource
  PreferencesService preferencesService;
  @Resource
  MainController mainController;
  @Resource
  AudioClipPlayer audioClipPlayer;
  @Resource
  ThemeService themeService;

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

  @PostConstruct
  void postConstruct() throws IOException {
    mainController.getRoot().sceneProperty().addListener((observable, oldValue, newValue) ->
        playSounds = newValue != null
    );

    notificationsPrefs = preferencesService.getPreferences().getNotification();
    notificationsPrefs.soundsEnabledProperty().addListener((observable, oldValue, newValue) ->
        playSounds &= newValue
    );

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
    return new AudioClip(themeService.getThemeFileUrl(sound).toString());
  }

  @Override
  public void playChatMentionSound() {
    if (!notificationsPrefs.isMentionSoundEnabled()) {
      return;
    }
    playSound(chatMentionSound);
  }

  @Override
  public void playPrivateMessageSound() {
    if (!notificationsPrefs.isPrivateMessageSoundEnabled()) {
      return;
    }
    playSound(privateMessageSound);
  }

  @Override
  public void playInfoNotificationSound() {
    if (!notificationsPrefs.isInfoSoundEnabled()) {
      return;
    }
    playSound(infoNotificationSound);
  }

  @Override
  public void playWarnNotificationSound() {
    if (!notificationsPrefs.isWarnSoundEnabled()) {
      return;
    }
    playSound(warnNotificationSound);
  }

  @Override
  public void playErrorNotificationSound() {
    if (!notificationsPrefs.isErrorSoundEnabled()) {
      return;
    }
    playSound(errorNotificationSound);
  }

  @Override
  public void playAchievementUnlockedSound() {
    playSound(achievementUnlockedSound);
  }

  @Override
  public void playFriendOnlineSound() {
    if (!notificationsPrefs.isFriendOnlineSoundEnabled()) {
      return;
    }
    playSound(friendOnlineSound);
  }

  @Override
  public void playFriendOfflineSound() {
    if (!notificationsPrefs.isFriendOfflineSoundEnabled()) {
      return;
    }
    playSound(friendOfflineSound);
  }

  @Override
  public void playFriendJoinsGameSound() {
    if (!notificationsPrefs.isFriendJoinsGameSoundEnabled()) {
      return;
    }
    playSound(friendJoinsGameSound);
  }

  @Override
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
