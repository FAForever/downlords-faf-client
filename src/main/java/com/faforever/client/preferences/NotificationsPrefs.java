package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class NotificationsPrefs {

  private final BooleanProperty soundsEnabled;
  private final BooleanProperty toastsEnabled;
  private final BooleanProperty mentionSoundEnabled;
  private final BooleanProperty infoSoundEnabled;
  private final BooleanProperty warnSoundEnabled;
  private final BooleanProperty errorSoundEnabled;
  private final BooleanProperty displayFriendOnlineToast;
  private final BooleanProperty displayFriendOfflineToast;
  private final BooleanProperty playFriendOnlineSound;
  private final BooleanProperty playFriendOfflineSound;
  private final BooleanProperty playFriendJoinsGameSound;
  private final BooleanProperty playFriendPlaysGameSound;
  private final BooleanProperty playPmReceivedSound;
  private final BooleanProperty displayPmReceivedToast;
  private final BooleanProperty displayFriendJoinsGameToast;

  public NotificationsPrefs() {
    soundsEnabled = new SimpleBooleanProperty(true);
    mentionSoundEnabled = new SimpleBooleanProperty(true);
    infoSoundEnabled = new SimpleBooleanProperty(true);
    warnSoundEnabled = new SimpleBooleanProperty(true);
    errorSoundEnabled = new SimpleBooleanProperty(true);
    toastsEnabled = new SimpleBooleanProperty(true);
    toastPosition = new SimpleObjectProperty<>(ToastPosition.BOTTOM_RIGHT);
    displayFriendOnlineToast = new SimpleBooleanProperty(true);
    displayFriendOfflineToast = new SimpleBooleanProperty(true);
    playFriendOnlineSound = new SimpleBooleanProperty(true);
    playFriendOfflineSound = new SimpleBooleanProperty(true);
    playFriendJoinsGameSound = new SimpleBooleanProperty(true);
    playFriendPlaysGameSound = new SimpleBooleanProperty(true);
    displayFriendJoinsGameToast = new SimpleBooleanProperty(true);
    displayFriendPlaysGameToast = new SimpleBooleanProperty(true);
    playPmReceivedSound = new SimpleBooleanProperty(true);
    displayPmReceivedToast = new SimpleBooleanProperty(true);
  }

  public boolean getPlayFriendJoinsGameSound() {
    return playFriendJoinsGameSound.get();
  }

  public BooleanProperty playFriendJoinsGameSoundProperty() {
    return playFriendJoinsGameSound;
  }

  public void setPlayFriendJoinsGameSound(boolean playFriendJoinsGameSound) {
    this.playFriendJoinsGameSound.set(playFriendJoinsGameSound);
  }

  public boolean getPlayFriendPlaysGameSound() {
    return playFriendPlaysGameSound.get();
  }

  public BooleanProperty playFriendPlaysGameSoundProperty() {
    return playFriendPlaysGameSound;
  }

  public void setPlayFriendPlaysGameSound(boolean playFriendPlaysGameSound) {
    this.playFriendPlaysGameSound.set(playFriendPlaysGameSound);
  }

  public boolean getDisplayFriendJoinsGameToast() {
    return displayFriendJoinsGameToast.get();
  }

  public BooleanProperty displayFriendJoinsGameToastProperty() {
    return displayFriendJoinsGameToast;
  }

  public void setDisplayFriendJoinsGameToast(boolean displayFriendJoinsGameToast) {
    this.displayFriendJoinsGameToast.set(displayFriendJoinsGameToast);
  }

  public boolean getDisplayFriendPlaysGameToast() {
    return displayFriendPlaysGameToast.get();
  }

  public BooleanProperty displayFriendPlaysGameToastProperty() {
    return displayFriendPlaysGameToast;
  }

  public void setDisplayFriendPlaysGameToast(boolean displayFriendPlaysGameToast) {
    this.displayFriendPlaysGameToast.set(displayFriendPlaysGameToast);
  }

  private final BooleanProperty displayFriendPlaysGameToast;
  private final ObjectProperty<ToastPosition> toastPosition;

  public boolean getDisplayFriendOnlineToast() {
    return displayFriendOnlineToast.get();
  }

  public BooleanProperty displayFriendOnlineToastProperty() {
    return displayFriendOnlineToast;
  }

  public void setDisplayFriendOnlineToast(boolean displayFriendOnlineToast) {
    this.displayFriendOnlineToast.set(displayFriendOnlineToast);
  }

  public boolean getDisplayFriendOfflineToast() {
    return displayFriendOfflineToast.get();
  }

  public BooleanProperty displayFriendOfflineToastProperty() {
    return displayFriendOfflineToast;
  }

  public void setDisplayFriendOfflineToast(boolean displayFriendOfflineToast) {
    this.displayFriendOfflineToast.set(displayFriendOfflineToast);
  }

  public boolean getPlayFriendOnlineSound() {
    return playFriendOnlineSound.get();
  }

  public BooleanProperty playFriendOnlineSoundProperty() {
    return playFriendOnlineSound;
  }

  public void setPlayFriendOnlineSound(boolean playFriendOnlineSound) {
    this.playFriendOnlineSound.set(playFriendOnlineSound);
  }

  public boolean getPlayFriendOfflineSound() {
    return playFriendOfflineSound.get();
  }

  public BooleanProperty playFriendOfflineSoundProperty() {
    return playFriendOfflineSound;
  }

  public void setPlayFriendOfflineSound(boolean playFriendOfflineSound) {
    this.playFriendOfflineSound.set(playFriendOfflineSound);
  }

  public boolean getToastsEnabled() {
    return toastsEnabled.get();
  }

  public BooleanProperty toastsEnabledProperty() {
    return toastsEnabled;
  }

  public void setToastsEnabled(boolean toastsEnabled) {
    this.toastsEnabled.set(toastsEnabled);
  }

  public ToastPosition getToastPosition() {
    return toastPosition.get();
  }

  public ObjectProperty<ToastPosition> toastPositionProperty() {
    return toastPosition;
  }

  public void setToastPosition(ToastPosition toastPosition) {
    this.toastPosition.set(toastPosition);
  }

  public boolean getSoundsEnabled() {
    return soundsEnabled.get();
  }

  public BooleanProperty soundsEnabledProperty() {
    return soundsEnabled;
  }

  public void setSoundsEnabled(boolean soundsEnabled) {
    this.soundsEnabled.set(soundsEnabled);
  }

  public boolean getMentionSoundEnabled() {
    return mentionSoundEnabled.get();
  }

  public BooleanProperty mentionSoundEnabledProperty() {
    return mentionSoundEnabled;
  }

  public void setMentionSoundEnabled(boolean mentionSoundEnabled) {
    this.mentionSoundEnabled.set(mentionSoundEnabled);
  }

  public boolean getInfoSoundEnabled() {
    return infoSoundEnabled.get();
  }

  public BooleanProperty infoSoundEnabledProperty() {
    return infoSoundEnabled;
  }

  public void setInfoSoundEnabled(boolean infoSoundEnabled) {
    this.infoSoundEnabled.set(infoSoundEnabled);
  }

  public boolean getWarnSoundEnabled() {
    return warnSoundEnabled.get();
  }

  public BooleanProperty warnSoundEnabledProperty() {
    return warnSoundEnabled;
  }

  public void setWarnSoundEnabled(boolean warnSoundEnabled) {
    this.warnSoundEnabled.set(warnSoundEnabled);
  }

  public boolean getErrorSoundEnabled() {
    return errorSoundEnabled.get();
  }

  public BooleanProperty errorSoundEnabledProperty() {
    return errorSoundEnabled;
  }

  public void setErrorSoundEnabled(boolean errorSoundEnabled) {
    this.errorSoundEnabled.set(errorSoundEnabled);
  }

  public boolean getPlayPmReceivedSound() {
    return playPmReceivedSound.get();
  }

  public BooleanProperty playPmReceivedSoundProperty() {
    return playPmReceivedSound;
  }

  public void setPlayPmReceivedSound(boolean playPmReceivedSound) {
    this.playPmReceivedSound.set(playPmReceivedSound);
  }

  public boolean getDisplayPmReceivedToast() {
    return displayPmReceivedToast.get();
  }

  public BooleanProperty displayPmReceivedToastProperty() {
    return displayPmReceivedToast;
  }

  public void setDisplayPmReceivedToast(boolean displayPmReceivedToast) {
    this.displayPmReceivedToast.set(displayPmReceivedToast);
  }
}
