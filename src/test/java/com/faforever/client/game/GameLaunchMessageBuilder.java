package com.faforever.client.game;

import com.faforever.client.remote.domain.GameLaunchMessage;

import java.util.Arrays;

public class GameLaunchMessageBuilder {

  private final GameLaunchMessage gameLaunchMessage;

  public GameLaunchMessageBuilder() {
    gameLaunchMessage = new GameLaunchMessage();
  }

  public GameLaunchMessageBuilder defaultValues() {
    gameLaunchMessage.setUid(1);
    gameLaunchMessage.setMod(GameType.DEFAULT.getString());
    gameLaunchMessage.setArgs(Arrays.asList("/ratingcolor red", "/clan foo"));
    return this;
  }

  public GameLaunchMessage get() {
    return gameLaunchMessage;
  }

  public static GameLaunchMessageBuilder create() {
    return new GameLaunchMessageBuilder();
  }
}
