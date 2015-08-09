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
  private final BooleanProperty friendOnlineSoundEnabled;
  private final BooleanProperty friendOfflineSoundEnabled;
  private final BooleanProperty friendJoinsGameSoundEnabled;
  private final BooleanProperty friendPlaysGameSoundEnabled;
  private final BooleanProperty privateMessageSoundEnabled;
  private final BooleanProperty privateMessageToastEnabled;
  private final BooleanProperty friendJoinsGameToastEnabled;
  private final BooleanProperty displayFriendPlaysGameToast;
  private final ObjectProperty<ToastPosition> toastPosition;

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
    friendOnlineSoundEnabled = new SimpleBooleanProperty(true);
    friendOfflineSoundEnabled = new SimpleBooleanProperty(true);
    friendJoinsGameSoundEnabled = new SimpleBooleanProperty(true);
    friendPlaysGameSoundEnabled = new SimpleBooleanProperty(true);
    friendJoinsGameToastEnabled = new SimpleBooleanProperty(true);
    displayFriendPlaysGameToast = new SimpleBooleanProperty(true);
    privateMessageSoundEnabled = new SimpleBooleanProperty(true);
    privateMessageToastEnabled = new SimpleBooleanProperty(true);
  }

  public boolean getFriendJoinsGameSoundEnabled() {
    return friendJoinsGameSoundEnabled.get();
  }

  public BooleanProperty friendJoinsGameSoundEnabledProperty() {
    return friendJoinsGameSoundEnabled;
  }

  public void setFriendJoinsGameSoundEnabled(boolean friendJoinsGameSoundEnabled) {
    this.friendJoinsGameSoundEnabled.set(friendJoinsGameSoundEnabled);
  }

  public boolean getFriendPlaysGameSoundEnabled() {
    return friendPlaysGameSoundEnabled.get();
  }

  public BooleanProperty friendPlaysGameSoundEnabledProperty() {
    return friendPlaysGameSoundEnabled;
  }

  public void setFriendPlaysGameSoundEnabled(boolean friendPlaysGameSoundEnabled) {
    this.friendPlaysGameSoundEnabled.set(friendPlaysGameSoundEnabled);
  }

  public boolean getFriendJoinsGameToastEnabled() {
    return friendJoinsGameToastEnabled.get();
  }

  public BooleanProperty friendJoinsGameToastEnabledProperty() {
    return friendJoinsGameToastEnabled;
  }

  public void setFriendJoinsGameToastEnabled(boolean friendJoinsGameToastEnabled) {
    this.friendJoinsGameToastEnabled.set(friendJoinsGameToastEnabled);
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

  public boolean getFriendOnlineSoundEnabled() {
    return friendOnlineSoundEnabled.get();
  }

  public BooleanProperty friendOnlineSoundEnabledProperty() {
    return friendOnlineSoundEnabled;
  }

  public void setFriendOnlineSoundEnabled(boolean friendOnlineSoundEnabled) {
    this.friendOnlineSoundEnabled.set(friendOnlineSoundEnabled);
  }

  public boolean getFriendOfflineSoundEnabled() {
    return friendOfflineSoundEnabled.get();
  }

  public BooleanProperty friendOfflineSoundEnabledProperty() {
    return friendOfflineSoundEnabled;
  }

  public void setFriendOfflineSoundEnabled(boolean friendOfflineSoundEnabled) {
    this.friendOfflineSoundEnabled.set(friendOfflineSoundEnabled);
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

  public boolean getPrivateMessageSoundEnabled() {
    return privateMessageSoundEnabled.get();
  }

  public BooleanProperty privateMessageSoundEnabledProperty() {
    return privateMessageSoundEnabled;
  }

  public void setPrivateMessageSoundEnabled(boolean privateMessageSoundEnabled) {
    this.privateMessageSoundEnabled.set(privateMessageSoundEnabled);
  }

  public boolean getPrivateMessageToastEnabled() {
    return privateMessageToastEnabled.get();
  }

  public BooleanProperty privateMessageToastEnabledProperty() {
    return privateMessageToastEnabled;
  }

  public void setPrivateMessageToastEnabled(boolean privateMessageToastEnabled) {
    this.privateMessageToastEnabled.set(privateMessageToastEnabled);
  }
}
