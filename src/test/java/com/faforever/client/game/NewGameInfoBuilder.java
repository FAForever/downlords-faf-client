package com.faforever.client.game;

import java.util.HashSet;

public class NewGameInfoBuilder {

  private final NewGameInfo newGameInfo;

  private NewGameInfoBuilder() {
    newGameInfo = new NewGameInfo();
  }

  public static NewGameInfoBuilder create() {
    return new NewGameInfoBuilder();
  }

  public NewGameInfoBuilder defaultValues() {
    newGameInfo.setMap("map");
    newGameInfo.setFeaturedMod(FeaturedModBeanBuilder.create().defaultValues().get());
    newGameInfo.setPassword("password");
    newGameInfo.setTitle("title");
    newGameInfo.setSimMods(new HashSet<>() {{
      add("111-456-789");
      add("222-456-789");
      add("333-456-789");
    }});
    return this;
  }

  public NewGameInfo get() {
    return newGameInfo;
  }


}
