package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Value;

@Value
public class NotificationsPrefs {

  BooleanProperty soundsEnabled = new SimpleBooleanProperty(true);
  BooleanProperty mentionSoundEnabled = new SimpleBooleanProperty(true);
  BooleanProperty infoSoundEnabled = new SimpleBooleanProperty(true);
  BooleanProperty warnSoundEnabled = new SimpleBooleanProperty(true);
  BooleanProperty errorSoundEnabled = new SimpleBooleanProperty(true);
  BooleanProperty transientNotificationsEnabled = new SimpleBooleanProperty(true);
  ObjectProperty<ToastPosition> toastPosition = new SimpleObjectProperty<>(ToastPosition.BOTTOM_RIGHT);
  BooleanProperty friendOnlineToastEnabled = new SimpleBooleanProperty(true);
  BooleanProperty friendOfflineToastEnabled = new SimpleBooleanProperty(true);
  BooleanProperty friendOnlineSoundEnabled = new SimpleBooleanProperty(true);
  BooleanProperty friendOfflineSoundEnabled = new SimpleBooleanProperty(true);
  BooleanProperty friendJoinsGameSoundEnabled = new SimpleBooleanProperty(true);
  BooleanProperty friendPlaysGameSoundEnabled = new SimpleBooleanProperty(true);
  BooleanProperty friendPlaysGameToastEnabled = new SimpleBooleanProperty(true);
  BooleanProperty friendJoinsGameToastEnabled = new SimpleBooleanProperty(true);
  BooleanProperty privateMessageSoundEnabled = new SimpleBooleanProperty(true);
  BooleanProperty privateMessageToastEnabled = new SimpleBooleanProperty(true);
  BooleanProperty notifyOnAtMentionOnlyEnabled = new SimpleBooleanProperty(false);
  IntegerProperty toastScreen = new SimpleIntegerProperty(0);
  IntegerProperty toastDisplayTime = new SimpleIntegerProperty(5000);
  BooleanProperty afterGameReviewEnabled = new SimpleBooleanProperty(true);

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

  public boolean getNotifyOnAtMentionOnlyEnabled() {
    return notifyOnAtMentionOnlyEnabled.get();
  }

  public void setNotifyOnAtMentionOnlyEnabled(boolean notifyOnAtMentionOnlyEnabled) {
    this.notifyOnAtMentionOnlyEnabled.set(notifyOnAtMentionOnlyEnabled);
  }

  public BooleanProperty notifyOnAtMentionOnlyEnabledProperty() {
    return notifyOnAtMentionOnlyEnabled;
  }

  public boolean isAfterGameReviewEnabled() {
    return afterGameReviewEnabled.get();
  }

  public void setAfterGameReviewEnabled(boolean afterGameReviewEnabled) {
    this.afterGameReviewEnabled.set(afterGameReviewEnabled);
  }

  public BooleanProperty afterGameReviewEnabledProperty() {
    return afterGameReviewEnabled;
  }
}
