package com.faforever.client.preferences;

public class NotificationPrefs {

  private boolean soundsEnabled;
  private boolean mentionSoundEnabled;
  private boolean pmSoundEnabled;
  private boolean infoSoundEnabled;
  private boolean warnSoundEnabled;
  private boolean errorSoundEnabled;

  public NotificationPrefs() {
    this.soundsEnabled = true;
    this.mentionSoundEnabled = true;
    this.pmSoundEnabled = true;
    this.infoSoundEnabled = true;
    this.warnSoundEnabled = true;
    this.errorSoundEnabled = true;
  }

  public boolean isSoundsEnabled() {
    return soundsEnabled;
  }

  public void setSoundsEnabled(boolean soundsEnabled) {
    this.soundsEnabled = soundsEnabled;
  }

  public boolean isMentionSoundEnabled() {
    return mentionSoundEnabled;
  }

  public void setMentionSoundEnabled(boolean mentionSoundEnabled) {
    this.mentionSoundEnabled = mentionSoundEnabled;
  }

  public boolean isPmSoundEnabled() {
    return pmSoundEnabled;
  }

  public void setPmSoundEnabled(boolean pmSoundEnabled) {
    this.pmSoundEnabled = pmSoundEnabled;
  }

  public boolean isInfoSoundEnabled() {
    return infoSoundEnabled;
  }

  public void setInfoSoundEnabled(boolean infoSoundEnabled) {
    this.infoSoundEnabled = infoSoundEnabled;
  }

  public boolean isWarnSoundEnabled() {
    return warnSoundEnabled;
  }

  public void setWarnSoundEnabled(boolean warnSoundEnabled) {
    this.warnSoundEnabled = warnSoundEnabled;
  }

  public boolean isErrorSoundEnabled() {
    return errorSoundEnabled;
  }

  public void setErrorSoundEnabled(boolean errorSoundEnabled) {
    this.errorSoundEnabled = errorSoundEnabled;
  }
}
