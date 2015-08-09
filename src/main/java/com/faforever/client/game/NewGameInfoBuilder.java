package com.faforever.client.game;

public class NewGameInfoBuilder {

  private final NewGameInfo newGameInfo;

  public NewGameInfoBuilder() {
    newGameInfo = new NewGameInfo();
  }

  public NewGameInfoBuilder defaultValues() {
    newGameInfo.setMap("map");
    newGameInfo.setMod("mod");
    newGameInfo.setPassword("password");
    newGameInfo.setTitle("title");
    newGameInfo.setVersion(1);
    return this;
  }

  public NewGameInfo get() {
    return newGameInfo;
  }

  public static NewGameInfoBuilder create() {
    return new NewGameInfoBuilder();
  }


}
