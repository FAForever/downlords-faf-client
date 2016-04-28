package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
  private final BooleanProperty displayRanked1v1Toast;
  private final BooleanProperty friendOnlineSoundEnabled;
  private final BooleanProperty friendOfflineSoundEnabled;
  private final BooleanProperty friendJoinsGameSoundEnabled;
  private final BooleanProperty friendPlaysGameSoundEnabled;
  private final BooleanProperty privateMessageSoundEnabled;
  private final BooleanProperty privateMessageToastEnabled;
  private final BooleanProperty friendJoinsGameToastEnabled;
  private final BooleanProperty displayFriendPlaysGameToast;
  private final ObjectProperty<ToastPosition> toastPosition;
  private final IntegerProperty toastScreen;
  private final IntegerProperty toastDisplayTime;

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
    displayRanked1v1Toast = new SimpleBooleanProperty(true);
    friendOnlineSoundEnabled = new SimpleBooleanProperty(true);
    friendOfflineSoundEnabled = new SimpleBooleanProperty(true);
    friendJoinsGameSoundEnabled = new SimpleBooleanProperty(true);
    friendPlaysGameSoundEnabled = new SimpleBooleanProperty(true);
    friendJoinsGameToastEnabled = new SimpleBooleanProperty(true);
    displayFriendPlaysGameToast = new SimpleBooleanProperty(true);
    privateMessageSoundEnabled = new SimpleBooleanProperty(true);
    privateMessageToastEnabled = new SimpleBooleanProperty(true);
    toastScreen = new SimpleIntegerProperty(0);
    toastDisplayTime = new SimpleIntegerProperty(5000);
  }

  public int getToastDisplayTime() {
    return toastDisplayTime.get();
  }

  public void setToastDisplayTime(int toastDisplayTime) {
    this.toastDisplayTime.set(toastDisplayTime);
  }

  public IntegerProperty toastDisplayTimeProperty() {
    return toastDisplayTime;
  }

  public int getToastScreen() {
    return toastScreen.get();
  }

  public void setToastScreen(int toastScreen) {
    this.toastScreen.set(toastScreen);
  }

  public IntegerProperty toastScreenProperty() {
    return toastScreen;
  }

  public boolean getFriendJoinsGameSoundEnabled() {
    return friendJoinsGameSoundEnabled.get();
  }

  public void setFriendJoinsGameSoundEnabled(boolean friendJoinsGameSoundEnabled) {
    this.friendJoinsGameSoundEnabled.set(friendJoinsGameSoundEnabled);
  }

  public BooleanProperty friendJoinsGameSoundEnabledProperty() {
    return friendJoinsGameSoundEnabled;
  }

  public boolean getFriendPlaysGameSoundEnabled() {
    return friendPlaysGameSoundEnabled.get();
  }

  public void setFriendPlaysGameSoundEnabled(boolean friendPlaysGameSoundEnabled) {
    this.friendPlaysGameSoundEnabled.set(friendPlaysGameSoundEnabled);
  }

  public BooleanProperty friendPlaysGameSoundEnabledProperty() {
    return friendPlaysGameSoundEnabled;
  }

  public boolean getFriendJoinsGameToastEnabled() {
    return friendJoinsGameToastEnabled.get();
  }

  public void setFriendJoinsGameToastEnabled(boolean friendJoinsGameToastEnabled) {
    this.friendJoinsGameToastEnabled.set(friendJoinsGameToastEnabled);
  }

  public BooleanProperty friendJoinsGameToastEnabledProperty() {
    return friendJoinsGameToastEnabled;
  }

  public boolean getDisplayFriendPlaysGameToast() {
    return displayFriendPlaysGameToast.get();
  }

  public void setDisplayFriendPlaysGameToast(boolean displayFriendPlaysGameToast) {
    this.displayFriendPlaysGameToast.set(displayFriendPlaysGameToast);
  }

  public BooleanProperty displayFriendPlaysGameToastProperty() {
    return displayFriendPlaysGameToast;
  }

  public boolean getDisplayFriendOnlineToast() {
    return displayFriendOnlineToast.get();
  }

  public void setDisplayFriendOnlineToast(boolean displayFriendOnlineToast) {
    this.displayFriendOnlineToast.set(displayFriendOnlineToast);
  }

  public BooleanProperty displayFriendOnlineToastProperty() {
    return displayFriendOnlineToast;
  }

  public boolean getDisplayFriendOfflineToast() {
    return displayFriendOfflineToast.get();
  }

  public void setDisplayFriendOfflineToast(boolean displayFriendOfflineToast) {
    this.displayFriendOfflineToast.set(displayFriendOfflineToast);
  }

  public BooleanProperty displayFriendOfflineToastProperty() {
    return displayFriendOfflineToast;
  }

  public boolean getFriendOnlineSoundEnabled() {
    return friendOnlineSoundEnabled.get();
  }

  public void setFriendOnlineSoundEnabled(boolean friendOnlineSoundEnabled) {
    this.friendOnlineSoundEnabled.set(friendOnlineSoundEnabled);
  }

  public BooleanProperty friendOnlineSoundEnabledProperty() {
    return friendOnlineSoundEnabled;
  }

  public boolean getFriendOfflineSoundEnabled() {
    return friendOfflineSoundEnabled.get();
  }

  public void setFriendOfflineSoundEnabled(boolean friendOfflineSoundEnabled) {
    this.friendOfflineSoundEnabled.set(friendOfflineSoundEnabled);
  }

  public BooleanProperty friendOfflineSoundEnabledProperty() {
    return friendOfflineSoundEnabled;
  }

  public boolean getToastsEnabled() {
    return toastsEnabled.get();
  }

  public void setToastsEnabled(boolean toastsEnabled) {
    this.toastsEnabled.set(toastsEnabled);
  }

  public BooleanProperty toastsEnabledProperty() {
    return toastsEnabled;
  }

  public ToastPosition getToastPosition() {
    return toastPosition.get();
  }

  public void setToastPosition(ToastPosition toastPosition) {
    this.toastPosition.set(toastPosition);
  }

  public ObjectProperty<ToastPosition> toastPositionProperty() {
    return toastPosition;
  }

  public boolean getSoundsEnabled() {
    return soundsEnabled.get();
  }

  public void setSoundsEnabled(boolean soundsEnabled) {
    this.soundsEnabled.set(soundsEnabled);
  }

  public BooleanProperty soundsEnabledProperty() {
    return soundsEnabled;
  }

  public boolean getMentionSoundEnabled() {
    return mentionSoundEnabled.get();
  }

  public void setMentionSoundEnabled(boolean mentionSoundEnabled) {
    this.mentionSoundEnabled.set(mentionSoundEnabled);
  }

  public BooleanProperty mentionSoundEnabledProperty() {
    return mentionSoundEnabled;
  }

  public boolean getInfoSoundEnabled() {
    return infoSoundEnabled.get();
  }

  public void setInfoSoundEnabled(boolean infoSoundEnabled) {
    this.infoSoundEnabled.set(infoSoundEnabled);
  }

  public BooleanProperty infoSoundEnabledProperty() {
    return infoSoundEnabled;
  }

  public boolean getWarnSoundEnabled() {
    return warnSoundEnabled.get();
  }

  public void setWarnSoundEnabled(boolean warnSoundEnabled) {
    this.warnSoundEnabled.set(warnSoundEnabled);
  }

  public BooleanProperty warnSoundEnabledProperty() {
    return warnSoundEnabled;
  }

  public boolean getErrorSoundEnabled() {
    return errorSoundEnabled.get();
  }

  public void setErrorSoundEnabled(boolean errorSoundEnabled) {
    this.errorSoundEnabled.set(errorSoundEnabled);
  }

  public BooleanProperty errorSoundEnabledProperty() {
    return errorSoundEnabled;
  }

  public boolean getPrivateMessageSoundEnabled() {
    return privateMessageSoundEnabled.get();
  }

  public void setPrivateMessageSoundEnabled(boolean privateMessageSoundEnabled) {
    this.privateMessageSoundEnabled.set(privateMessageSoundEnabled);
  }

  public BooleanProperty privateMessageSoundEnabledProperty() {
    return privateMessageSoundEnabled;
  }

  public boolean getPrivateMessageToastEnabled() {
    return privateMessageToastEnabled.get();
  }

  public void setPrivateMessageToastEnabled(boolean privateMessageToastEnabled) {
    this.privateMessageToastEnabled.set(privateMessageToastEnabled);
  }

  public BooleanProperty privateMessageToastEnabledProperty() {
    return privateMessageToastEnabled;
  }

  public boolean getDisplayRanked1v1Toast() {
    return displayRanked1v1Toast.get();
  }

  public void setDisplayRanked1v1Toast(boolean displayRanked1v1Toast) {
    this.displayRanked1v1Toast.set(displayRanked1v1Toast);
  }

  public BooleanProperty displayRanked1v1ToastProperty() {
    return displayRanked1v1Toast;
  }
}
