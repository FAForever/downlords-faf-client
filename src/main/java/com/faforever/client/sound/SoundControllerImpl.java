package com.faforever.client.sound;

import com.faforever.client.main.MainController;
import com.faforever.client.preferences.NotificationsPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.ThemeUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.media.AudioClip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class SoundControllerImpl implements SoundController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  MainController mainController;

  @Autowired
  AudioClipPlayer audioClipPlayer;

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
    notificationsPrefs.soundsEnabledProperty().addListener(new ChangeListener<Boolean>() {
      @Override
      public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        playSounds &= newValue;
      }
    });

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
