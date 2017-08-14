package com.faforever.client.mod.event;

import com.faforever.client.mod.Mod;

public class ModUploadedEvent {
  private final Mod modInfo;

  public ModUploadedEvent(Mod modInfo) {
    this.modInfo = modInfo;
  }

  public Mod getModInfo() {
    return modInfo;
  }
}
