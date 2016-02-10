package com.faforever.client.game;

import com.faforever.client.legacy.domain.GameInfoMessage;
import com.faforever.client.legacy.domain.GameState;

public class GameInfoMessageBuilder {

  private GameInfoMessage gameInfoMessage;

  private GameInfoMessageBuilder(Integer uid) {
    gameInfoMessage = new GameInfoMessage();
    gameInfoMessage.setUid(uid);
  }

  public GameInfoMessageBuilder defaultValues() {
    gameInfoMessage.setHost("Some host");
    gameInfoMessage.setFeaturedMod(GameType.FAF.getString());
    gameInfoMessage.setMapname("scmp_007");
    gameInfoMessage.setMaxPlayers(4);
    gameInfoMessage.setNumPlayers(1);
    gameInfoMessage.setState(GameState.OPEN);
    gameInfoMessage.setTitle("Test game");
    gameInfoMessage.setPasswordProtected(false);
    return this;
  }

  public GameInfoMessage get() {
    return gameInfoMessage;
  }

  public static GameInfoMessageBuilder create(Integer uid) {
    return new GameInfoMessageBuilder(uid);
  }
}
