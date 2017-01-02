package com.faforever.client.game;

import com.faforever.client.mod.FeaturedMod;

public class FeaturedModBeanBuilder {

  private final FeaturedMod featuredMod;

  private FeaturedModBeanBuilder() {
    featuredMod = new FeaturedMod();
  }

  public static FeaturedModBeanBuilder create() {
    return new FeaturedModBeanBuilder();
  }

  public FeaturedModBeanBuilder defaultValues() {
    featuredMod.setTechnicalName("faf");
    featuredMod.setVisible(true);
    featuredMod.setDescription("Standard mod");
    featuredMod.setDisplayName("Forged Alliance Forever");
    featuredMod.setGitUrl("http://localhost/example.git");
    return this;
  }

  public FeaturedMod get() {
    return featuredMod;
  }

  public FeaturedModBeanBuilder technicalName(String technicalName) {
    featuredMod.setTechnicalName(technicalName);
    return this;
  }
}
