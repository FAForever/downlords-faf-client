package com.faforever.client.mod;

import com.faforever.client.api.Mod;

public class ModInfoBuilder {

  private final Mod mod;

  public ModInfoBuilder() {
    mod = new Mod();
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

  public static ModInfoBuilder create() {
    return new ModInfoBuilder();
  }
}
