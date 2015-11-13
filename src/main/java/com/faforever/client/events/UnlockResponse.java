package com.faforever.client.events;

import com.google.api.client.util.Key;

public class UnlockResponse {

  @Key("newly_unlocked")
  private boolean newlyUnlocked;

  public boolean isNewlyUnlocked() {
    return newlyUnlocked;
  }

  public void setNewlyUnlocked(boolean newlyUnlocked) {
    this.newlyUnlocked = newlyUnlocked;
  }
}
