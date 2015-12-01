package com.faforever.client.audio;

import com.faforever.client.ThemeService;
import com.faforever.client.main.MainController;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.PreferencesService;
import javafx.scene.media.AudioClip;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;

public class AudioControllerImpl implements AudioController {

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
  private AudioClip errorNotificationSound;
  private AudioClip infoNotificationSound;
  private AudioClip warnNotificationSound;
  private AudioClip privateMessageSound;

  private boolean playSounds;
  private NotificationsPrefs notificationsPrefs;

  @PostConstruct
  void postConstruct() throws IOException {
    mainController.getRoot().sceneProperty().addListener((observable, oldValue, newValue) -> {
      playSounds = newValue != null;
    });

    notificationsPrefs = preferencesService.getPreferences().getNotification();
    notificationsPrefs.soundsEnabledProperty().addListener((observable, oldValue, newValue) -> {
      playSounds &= newValue;
    });

    loadSounds();
  }

  private void loadSounds() throws IOException {
    infoNotificationSound = loadSound(INFO_SOUND);
    errorNotificationSound = loadSound(INFO_SOUND);
    warnNotificationSound = loadSound(INFO_SOUND);
    chatMentionSound = loadSound(MENTION_SOUND);
    privateMessageSound = loadSound(PRIVATE_MESSAGE_SOUND);
  }

  private AudioClip loadSound(String sound) throws IOException {
    return new AudioClip(new ClassPathResource(themeService.getThemeFile(sound)).getURL().toString());
  }

  @Override
  public void playChatMentionSound() {
    if (!notificationsPrefs.getMentionSoundEnabled()) {
      return;
    }
    playSound(chatMentionSound);
  }

  @Override
  public void playPrivateMessageSound() {
    if (!notificationsPrefs.getPrivateMessageSoundEnabled()) {
      return;
    }
    playSound(privateMessageSound);
  }

  @Override
  public void playInfoNotificationSound() {
    if (!notificationsPrefs.getInfoSoundEnabled()) {
      return;
    }
    playSound(infoNotificationSound);
  }

  @Override
  public void playWarnNotificationSound() {
    if (!notificationsPrefs.getWarnSoundEnabled()) {
      return;
    }
    playSound(warnNotificationSound);
  }

  @Override
  public void playErrorNotificationSound() {
    if (!notificationsPrefs.getErrorSoundEnabled()) {
      return;
    }
    playSound(errorNotificationSound);
  }

  void playSound(AudioClip audioClip) {
    if (!playSounds) {
      return;
    }

    audioClipPlayer.playSound(audioClip);
  }
}
