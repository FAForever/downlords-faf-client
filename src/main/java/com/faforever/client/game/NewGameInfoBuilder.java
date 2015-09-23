package com.faforever.client.game;

import java.util.HashSet;

public class NewGameInfoBuilder {

  private final NewGameInfo newGameInfo;

  public NewGameInfoBuilder() {
    newGameInfo = new NewGameInfo();
  }

  public NewGameInfoBuilder defaultValues() {
    newGameInfo.setMap("map");
    newGameInfo.setGameType("mod");
    newGameInfo.setPassword("password");
    newGameInfo.setTitle("title");
    newGameInfo.setVersion(1);
    newGameInfo.setSimModUidsToVersions(new HashSet<String>() {{
      add("111-456-789");
      add("222-456-789");
      add("333-456-789");
    }});
    return this;
  }

  public NewGameInfo get() {
    return newGameInfo;
  }

  public static NewGameInfoBuilder create() {
    return new NewGameInfoBuilder();
  }


}
