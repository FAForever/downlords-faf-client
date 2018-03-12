package com.faforever.client.mod.event;

import com.faforever.client.mod.ModVersion;

public class ModUploadedEvent {
  private final ModVersion modVersionInfo;

  public ModUploadedEvent(ModVersion modVersionInfo) {
    this.modVersionInfo = modVersionInfo;
  }

  public ModVersion getModVersionInfo() {
    return modVersionInfo;
  }
}
