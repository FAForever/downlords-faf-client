package com.faforever.client.game;

import com.faforever.client.legacy.domain.GameLaunchInfo;

import java.util.Arrays;

public class GameLaunchInfoBuilder {

  private final GameLaunchInfo gameLaunchInfo;

  public GameLaunchInfoBuilder() {
    gameLaunchInfo = new GameLaunchInfo();
  }

  public GameLaunchInfoBuilder defaultValues() {
    gameLaunchInfo.setVersion("1");
    gameLaunchInfo.setUid(1);
    gameLaunchInfo.setMod("mod");
    gameLaunchInfo.setArgs(Arrays.asList("/ratingcolor red", "/clan foo"));
    return this;
  }

  public GameLaunchInfo get() {
    return gameLaunchInfo;
  }

  public static GameLaunchInfoBuilder create() {
    return new GameLaunchInfoBuilder();
  }
}
