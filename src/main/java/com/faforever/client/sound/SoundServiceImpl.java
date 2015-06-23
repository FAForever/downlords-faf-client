package com.faforever.client.sound;

import com.faforever.client.main.MainController;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.ThemeUtil;
import javafx.scene.media.AudioClip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class SoundServiceImpl implements SoundService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  MainController mainController;

  private AudioClip chatMentionSound;
  private AudioClip errorNotificationSound;
  private AudioClip infoNotificationSound;
  private AudioClip warnNotificationSound;
  private AudioClip privateMessageSound;
  private boolean playSounds;
  private boolean isUiLoaded;
  private NotificationPrefs notificationPrefs;

  @PostConstruct
  void postConstruct() throws IOException {
    mainController.getRoot().sceneProperty().addListener((observable, oldValue, newValue) -> {
      playSounds = newValue != null
          && preferencesService.getPreferences().getNotificationPrefs().isSoundsEnabled();
    });
    preferencesService.addUpdateListener(preferences -> {
      try {
        notificationPrefs = preferences.getNotificationPrefs();
        loadSounds();
      } catch (IOException e) {
        logger.warn("Notification sounds could not be loaded", e);
      }
    });


    notificationPrefs = preferencesService.getPreferences().getNotificationPrefs();

    loadSounds();
  }

  private void loadSounds() throws IOException {
    String theme = preferencesService.getPreferences().getTheme();

    errorNotificationSound = loadSound(theme, "sounds/error.mp3");
    infoNotificationSound = loadSound(theme, "sounds/info.mp3");
    chatMentionSound = loadSound(theme, "sounds/mention.mp3");
    privateMessageSound = loadSound(theme, "sounds/pm.mp3");
    warnNotificationSound = loadSound(theme, "sounds/warn.mp3");
  }

  private AudioClip loadSound(String theme, String sound) throws IOException {
    return new AudioClip(new ClassPathResource(ThemeUtil.themeFile(theme, sound)).getURL().toString());
  }

  @Override
  public void playChatMentionSound() {
    if (!notificationPrefs.isMentionSoundEnabled()) {
      return;
    }
    playSound(chatMentionSound);
  }

  @Override
  public void playPrivateMessageSound() {
    if (!notificationPrefs.isPmSoundEnabled()) {
      return;
    }
    playSound(privateMessageSound);
  }

  @Override
  public void playInfoNotificationSound() {
    if (!notificationPrefs.isInfoSoundEnabled()) {
      return;
    }
    playSound(infoNotificationSound);
  }

  @Override
  public void playWarnNotificationSound() {
    if (!notificationPrefs.isWarnSoundEnabled()) {
      return;
    }
    playSound(warnNotificationSound);
  }

  @Override
  public void playErrorNotificationSound() {
    if (!notificationPrefs.isErrorSoundEnabled()) {
      return;
    }
    playSound(errorNotificationSound);
  }

  private void playSound(AudioClip audioClip) {
    if (!playSounds) {
      return;
    }

    audioClip.play();
  }
}
