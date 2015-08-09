package com.faforever.client.game;

import com.faforever.client.legacy.domain.GameTypeInfo;

public class GameTypeInfoBuilder {

  private final GameTypeInfo gameTypeInfo;

  public GameTypeInfoBuilder() {
    gameTypeInfo = new GameTypeInfo();
  }

  public GameTypeInfoBuilder defaultValues() {
    gameTypeInfo.setDesc("Description");
    gameTypeInfo.setFullname("Full name");
    gameTypeInfo.setHost(true);
    gameTypeInfo.setIcon("icon");
    gameTypeInfo.setJoin(true);
    gameTypeInfo.setLive(true);
    gameTypeInfo.setOptions(new Boolean[0]);
    return this;
  }

  public GameTypeInfo get() {
    return gameTypeInfo;
  }

  public static GameTypeInfoBuilder create() {
    return new GameTypeInfoBuilder();
  }
}
