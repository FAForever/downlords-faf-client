package com.faforever.client.game;

import com.faforever.client.legacy.domain.GameTypeInfo;

public class GameTypeInfoBuilder {

  private final GameTypeInfo gameTypeInfo;

  public GameTypeInfoBuilder() {
    gameTypeInfo = new GameTypeInfo();
  }

  public GameTypeInfoBuilder defaultValues() {
    gameTypeInfo.desc = "Description";
    gameTypeInfo.fullname = "Full name";
    gameTypeInfo.host = true;
    gameTypeInfo.icon = "icon";
    gameTypeInfo.join = true;
    gameTypeInfo.live = true;
    gameTypeInfo.options = new Boolean[0];
    gameTypeInfo.command = "command";
    return this;
  }

  public GameTypeInfo get() {
    return gameTypeInfo;
  }

  public static GameTypeInfoBuilder create() {
    return new GameTypeInfoBuilder();
  }
}
