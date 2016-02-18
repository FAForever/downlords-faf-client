package com.faforever.client.game;

import com.faforever.client.remote.domain.GameTypeMessage;

public class GameTypeInfoBuilder {

  private final GameTypeMessage gameTypeMessage;

  public GameTypeInfoBuilder() {
    gameTypeMessage = new GameTypeMessage();
  }

  public GameTypeInfoBuilder defaultValues() {
    gameTypeMessage.setDesc("Description");
    gameTypeMessage.setFullname("Full name");
    gameTypeMessage.setIcon("icon");
    gameTypeMessage.setJoin(true);
    gameTypeMessage.setPublish(true);
    gameTypeMessage.setOptions(new Boolean[0]);
    return this;
  }

  public GameTypeMessage get() {
    return gameTypeMessage;
  }

  public static GameTypeInfoBuilder create() {
    return new GameTypeInfoBuilder();
  }
}
