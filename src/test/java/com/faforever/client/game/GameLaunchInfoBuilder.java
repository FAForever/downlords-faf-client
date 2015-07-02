package com.faforever.client.game;

import com.faforever.client.legacy.domain.GameLaunchInfo;

import java.util.Arrays;

public class GameLaunchInfoBuilder {

  private final GameLaunchInfo gameLaunchInfo;

  public GameLaunchInfoBuilder defaultValues() {
    gameLaunchInfo.version = "1";
    gameLaunchInfo.uid = 1;
    gameLaunchInfo.mod = "mod";
    gameLaunchInfo.args = Arrays.asList("/ratingcolor red", "/clan foo");
    return this;
  }

  public GameLaunchInfo get() {
    return gameLaunchInfo;
  }

  public static GameLaunchInfoBuilder create() {
    return new GameLaunchInfoBuilder();
  }

  public GameLaunchInfoBuilder() {
    gameLaunchInfo = new GameLaunchInfo();
  }
}
