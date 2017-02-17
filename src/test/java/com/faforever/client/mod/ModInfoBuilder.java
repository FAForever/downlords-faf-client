package com.faforever.client.mod;

import com.faforever.client.api.dto.Mod;

public class ModInfoBuilder {

  private final Mod mod;

  public ModInfoBuilder() {
    mod = new Mod();
  }

  public static ModInfoBuilder create() {
    return new ModInfoBuilder();
  }

  public ModInfoBuilder defaultValues() {
    return this;
  }

  public ModInfoBuilder id(String id) {
    mod.setId(id);
    return this;
  }

  public Mod get() {
    return mod;
  }
}
