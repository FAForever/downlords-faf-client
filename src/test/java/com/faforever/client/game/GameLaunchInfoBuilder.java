package com.faforever.client.game;

import com.faforever.client.legacy.domain.GameLaunchMessageLobby;

import java.util.Arrays;

public class GameLaunchInfoBuilder {

  private final GameLaunchMessageLobby gameLaunchMessage;

  public GameLaunchInfoBuilder() {
    gameLaunchMessage = new GameLaunchMessageLobby();
  }

  public GameLaunchInfoBuilder defaultValues() {
    gameLaunchMessage.setUid(1);
    gameLaunchMessage.setMod("mod");
    gameLaunchMessage.setArgs(Arrays.asList("/ratingcolor red", "/clan foo"));
    return this;
  }

  public GameLaunchMessageLobby get() {
    return gameLaunchMessage;
  }

  public static GameLaunchInfoBuilder create() {
    return new GameLaunchInfoBuilder();
  }
}
