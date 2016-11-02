package com.faforever.client.game;

import com.faforever.client.remote.domain.FeaturedModMessage;

public class GameTypeInfoBuilder {

  private final FeaturedModMessage featuredModMessage;

  public GameTypeInfoBuilder() {
    featuredModMessage = new FeaturedModMessage();
  }

  public static GameTypeInfoBuilder create() {
    return new GameTypeInfoBuilder();
  }

  public GameTypeInfoBuilder defaultValues() {
    featuredModMessage.setDesc("Description");
    featuredModMessage.setFullname("Full name");
    featuredModMessage.setIcon("icon");
    featuredModMessage.setJoin(true);
    featuredModMessage.setPublish(true);
    featuredModMessage.setOptions(new Boolean[0]);
    return this;
  }

  public FeaturedModMessage get() {
    return featuredModMessage;
  }
}
