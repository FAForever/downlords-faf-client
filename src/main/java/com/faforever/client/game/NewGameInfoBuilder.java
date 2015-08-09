package com.faforever.client.game;

public class NewGameInfoBuilder {

  private final NewGameInfo newGameInfo;

  public NewGameInfoBuilder() {
    newGameInfo = new NewGameInfo();
  }

  public NewGameInfoBuilder defaultValues() {
    newGameInfo.map = "map";
    newGameInfo.mod = "mod";
    newGameInfo.password = "password";
    newGameInfo.title = "title";
    newGameInfo.version = 1;
    return this;
  }

  public NewGameInfo get() {
    return newGameInfo;
  }

  public static NewGameInfoBuilder create() {
    return new NewGameInfoBuilder();
  }


}
