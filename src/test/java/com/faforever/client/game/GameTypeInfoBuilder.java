package com.faforever.client.game;

import com.faforever.client.api.dto.FeaturedMod;

public class GameTypeInfoBuilder {

  private final FeaturedMod featuredMod;

  public GameTypeInfoBuilder() {
    featuredMod = new FeaturedMod();
  }

  public static GameTypeInfoBuilder create() {
    return new GameTypeInfoBuilder();
  }

  public GameTypeInfoBuilder defaultValues() {
    featuredMod.setDescription("Description");
    featuredMod.setDisplayName("Full name");
    featuredMod.setVisible(true);
    return this;
  }

  public FeaturedMod get() {
    return featuredMod;
  }
}
