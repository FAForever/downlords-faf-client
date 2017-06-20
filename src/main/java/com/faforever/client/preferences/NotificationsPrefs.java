package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public class NotificationsPrefs {

  private final BooleanProperty soundsEnabled;
  private final BooleanProperty transientNotificationsEnabled;
  private final BooleanProperty mentionSoundEnabled;
  private final BooleanProperty infoSoundEnabled;
  private final BooleanProperty warnSoundEnabled;
  private final BooleanProperty errorSoundEnabled;
  private final BooleanProperty friendOnlineToastEnabled;
  private final BooleanProperty friendOfflineToastEnabled;
  private final BooleanProperty ladder1v1ToastEnabled;
  private final BooleanProperty friendOnlineSoundEnabled;
  private final BooleanProperty friendOfflineSoundEnabled;
  private final BooleanProperty friendJoinsGameSoundEnabled;
  private final BooleanProperty friendPlaysGameSoundEnabled;
  private final BooleanProperty friendPlaysGameToastEnabled;
  private final BooleanProperty privateMessageSoundEnabled;
  private final BooleanProperty privateMessageToastEnabled;
  private final BooleanProperty friendJoinsGameToastEnabled;
  private final BooleanProperty notifyOnAtMentionEnabled;
  private final ObjectProperty<ToastPosition> toastPosition;
  private final IntegerProperty toastScreen;
  private final IntegerProperty toastDisplayTime;

  public NotificationsPrefs() {
    soundsEnabled = new SimpleBooleanProperty(true);
    mentionSoundEnabled = new SimpleBooleanProperty(true);
    infoSoundEnabled = new SimpleBooleanProperty(true);
    warnSoundEnabled = new SimpleBooleanProperty(true);
    errorSoundEnabled = new SimpleBooleanProperty(true);
    transientNotificationsEnabled = new SimpleBooleanProperty(true);
    toastPosition = new SimpleObjectProperty<>(ToastPosition.BOTTOM_RIGHT);
    friendOnlineToastEnabled = new SimpleBooleanProperty(true);
    friendOfflineToastEnabled = new SimpleBooleanProperty(true);
    ladder1v1ToastEnabled = new SimpleBooleanProperty(true);
    friendOnlineSoundEnabled = new SimpleBooleanProperty(true);
    friendOfflineSoundEnabled = new SimpleBooleanProperty(true);
    friendJoinsGameSoundEnabled = new SimpleBooleanProperty(true);
    friendPlaysGameSoundEnabled = new SimpleBooleanProperty(true);
    friendPlaysGameToastEnabled = new SimpleBooleanProperty(true);
    friendJoinsGameToastEnabled = new SimpleBooleanProperty(true);
    privateMessageSoundEnabled = new SimpleBooleanProperty(true);
    privateMessageToastEnabled = new SimpleBooleanProperty(true);
    notifyOnAtMentionEnabled = new SimpleBooleanProperty(false);
    toastScreen = new SimpleIntegerProperty(0);
    toastDisplayTime = new SimpleIntegerProperty(5000);
  }

  public boolean isSoundsEnabled() {
    return soundsEnabled.get();
  }

  public void setSoundsEnabled(boolean soundsEnabled) {
    this.soundsEnabled.set(soundsEnabled);
  }

  public BooleanProperty soundsEnabledProperty() {
    return soundsEnabled;
  }

  public boolean isTransientNotificationsEnabled() {
    return transientNotificationsEnabled.get();
  }

  public void setTransientNotificationsEnabled(boolean transientNotificationsEnabled) {
    this.transientNotificationsEnabled.set(transientNotificationsEnabled);
  }

  public BooleanProperty transientNotificationsEnabledProperty() {
    return transientNotificationsEnabled;
  }

  public boolean isMentionSoundEnabled() {
    return mentionSoundEnabled.get();
  }

  public void setMentionSoundEnabled(boolean mentionSoundEnabled) {
    this.mentionSoundEnabled.set(mentionSoundEnabled);
  }

  public BooleanProperty mentionSoundEnabledProperty() {
    return mentionSoundEnabled;
  }

  public boolean isInfoSoundEnabled() {
    return infoSoundEnabled.get();
  }

  public void setInfoSoundEnabled(boolean infoSoundEnabled) {
    this.infoSoundEnabled.set(infoSoundEnabled);
  }

  public BooleanProperty infoSoundEnabledProperty() {
    return infoSoundEnabled;
  }

  public boolean isWarnSoundEnabled() {
    return warnSoundEnabled.get();
  }

  public void setWarnSoundEnabled(boolean warnSoundEnabled) {
    this.warnSoundEnabled.set(warnSoundEnabled);
  }

  public BooleanProperty warnSoundEnabledProperty() {
    return warnSoundEnabled;
  }

  public boolean isErrorSoundEnabled() {
    return errorSoundEnabled.get();
  }

  public void setErrorSoundEnabled(boolean errorSoundEnabled) {
    this.errorSoundEnabled.set(errorSoundEnabled);
  }

  public BooleanProperty errorSoundEnabledProperty() {
    return errorSoundEnabled;
  }

  public boolean isFriendOnlineToastEnabled() {
    return friendOnlineToastEnabled.get();
  }

  public void setFriendOnlineToastEnabled(boolean friendOnlineToastEnabled) {
    this.friendOnlineToastEnabled.set(friendOnlineToastEnabled);
  }

  public BooleanProperty friendOnlineToastEnabledProperty() {
    return friendOnlineToastEnabled;
  }

  public boolean isFriendOfflineToastEnabled() {
    return friendOfflineToastEnabled.get();
  }

  public void setFriendOfflineToastEnabled(boolean friendOfflineToastEnabled) {
    this.friendOfflineToastEnabled.set(friendOfflineToastEnabled);
  }

  public BooleanProperty friendOfflineToastEnabledProperty() {
    return friendOfflineToastEnabled;
  }

  public boolean getLadder1v1ToastEnabled() {
    return ladder1v1ToastEnabled.get();
  }

  public void setLadder1v1ToastEnabled(boolean ladder1v1ToastEnabled) {
    this.ladder1v1ToastEnabled.set(ladder1v1ToastEnabled);
  }

  public BooleanProperty ladder1v1ToastEnabledProperty() {
    return ladder1v1ToastEnabled;
  }

  public boolean isFriendOnlineSoundEnabled() {
    return friendOnlineSoundEnabled.get();
  }

  public void setFriendOnlineSoundEnabled(boolean friendOnlineSoundEnabled) {
    this.friendOnlineSoundEnabled.set(friendOnlineSoundEnabled);
  }

  public BooleanProperty friendOnlineSoundEnabledProperty() {
    return friendOnlineSoundEnabled;
  }

  public boolean isFriendOfflineSoundEnabled() {
    return friendOfflineSoundEnabled.get();
  }

  public void setFriendOfflineSoundEnabled(boolean friendOfflineSoundEnabled) {
    this.friendOfflineSoundEnabled.set(friendOfflineSoundEnabled);
  }

  public BooleanProperty friendOfflineSoundEnabledProperty() {
    return friendOfflineSoundEnabled;
  }

  public boolean isFriendJoinsGameSoundEnabled() {
    return friendJoinsGameSoundEnabled.get();
  }

  public void setFriendJoinsGameSoundEnabled(boolean friendJoinsGameSoundEnabled) {
    this.friendJoinsGameSoundEnabled.set(friendJoinsGameSoundEnabled);
  }

  public BooleanProperty friendJoinsGameSoundEnabledProperty() {
    return friendJoinsGameSoundEnabled;
  }

  public boolean isFriendPlaysGameSoundEnabled() {
    return friendPlaysGameSoundEnabled.get();
  }

  public void setFriendPlaysGameSoundEnabled(boolean friendPlaysGameSoundEnabled) {
    this.friendPlaysGameSoundEnabled.set(friendPlaysGameSoundEnabled);
  }

  public BooleanProperty friendPlaysGameSoundEnabledProperty() {
    return friendPlaysGameSoundEnabled;
  }

  public boolean isFriendPlaysGameToastEnabled() {
    return friendPlaysGameToastEnabled.get();
  }

  public void setFriendPlaysGameToastEnabled(boolean friendPlaysGameToastEnabled) {
    this.friendPlaysGameToastEnabled.set(friendPlaysGameToastEnabled);
  }

  public BooleanProperty friendPlaysGameToastEnabledProperty() {
    return friendPlaysGameToastEnabled;
  }

  public boolean isPrivateMessageSoundEnabled() {
    return privateMessageSoundEnabled.get();
  }

  public void setPrivateMessageSoundEnabled(boolean privateMessageSoundEnabled) {
    this.privateMessageSoundEnabled.set(privateMessageSoundEnabled);
  }

  public BooleanProperty privateMessageSoundEnabledProperty() {
    return privateMessageSoundEnabled;
  }

  public boolean isPrivateMessageToastEnabled() {
    return privateMessageToastEnabled.get();
  }

  public void setPrivateMessageToastEnabled(boolean privateMessageToastEnabled) {
    this.privateMessageToastEnabled.set(privateMessageToastEnabled);
  }

  public BooleanProperty privateMessageToastEnabledProperty() {
    return privateMessageToastEnabled;
  }

  public boolean isFriendJoinsGameToastEnabled() {
    return friendJoinsGameToastEnabled.get();
  }

  public void setFriendJoinsGameToastEnabled(boolean friendJoinsGameToastEnabled) {
    this.friendJoinsGameToastEnabled.set(friendJoinsGameToastEnabled);
  }

  public BooleanProperty friendJoinsGameToastEnabledProperty() {
    return friendJoinsGameToastEnabled;
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

  public int getToastScreen() {
    return toastScreen.get();
  }

  public void setToastScreen(int toastScreen) {
    this.toastScreen.set(toastScreen);
  }

  public IntegerProperty toastScreenProperty() {
    return toastScreen;
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

  public boolean isNotifyOnAtMentionEnabled() {
    return notifyOnAtMentionEnabled.get();
  }

  public BooleanProperty notifyOnAtMentionEnabledProperty() {
    return notifyOnAtMentionEnabled;
  }
}
