package com.faforever.client.mod;

import com.faforever.client.legacy.domain.ModInfo;

import java.util.ArrayList;

public class ModInfoBuilder {

  private final ModInfo modInfo;

  public ModInfoBuilder() {
    modInfo = new ModInfo();
  }

  public ModInfoBuilder defaultValues() {
    modInfo.setComments(new ArrayList<>());
    return this;
  }

  public ModInfoBuilder uid(String uid) {
    modInfo.setUid(uid);
    return this;
  }

  public ModInfo get() {
    return modInfo;
  }

  public static ModInfoBuilder create() {
    return new ModInfoBuilder();
  }
}
