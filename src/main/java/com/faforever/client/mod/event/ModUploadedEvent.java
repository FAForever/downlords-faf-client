package com.faforever.client.mod.event;

import com.faforever.client.mod.ModInfoBean;

public class ModUploadedEvent {
  private final ModInfoBean modInfo;

  public ModUploadedEvent(ModInfoBean modInfo) {
    this.modInfo = modInfo;
  }

  public ModInfoBean getModInfo() {
    return modInfo;
  }
}
