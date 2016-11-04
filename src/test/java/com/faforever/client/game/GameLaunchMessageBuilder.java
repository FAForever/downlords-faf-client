package com.faforever.client.game;

import com.faforever.client.remote.domain.GameLaunchMessage;

import java.util.Arrays;

public class GameLaunchMessageBuilder {

  private final GameLaunchMessage gameLaunchMessage;

  public GameLaunchMessageBuilder() {
    gameLaunchMessage = new GameLaunchMessage();
  }

  public static GameLaunchMessageBuilder create() {
    return new GameLaunchMessageBuilder();
  }

  public GameLaunchMessageBuilder defaultValues() {
    gameLaunchMessage.setUid(1);
    gameLaunchMessage.setMod(KnownFeaturedMod.DEFAULT.getString());
    gameLaunchMessage.setArgs(Arrays.asList("/ratingcolor red", "/clan foo"));
    return this;
  }

  public GameLaunchMessage get() {
    return gameLaunchMessage;
  }
}
