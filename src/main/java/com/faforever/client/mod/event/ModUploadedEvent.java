package com.faforever.client.mod.event;

import com.faforever.client.domain.ModVersionBean;

public class ModUploadedEvent {
  private final ModVersionBean modVersionInfo;

  public ModUploadedEvent(ModVersionBean modVersionInfo) {
    this.modVersionInfo = modVersionInfo;
  }

  public ModVersionBean getModVersionInfo() {
    return modVersionInfo;
  }
}
