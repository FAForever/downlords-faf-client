package com.faforever.client.legacy.domain;

import java.util.List;

public class GameLaunchInfo extends ServerObject {

  /**
   * Stores game launch arguments, like "/ratingcolor d8d8d8d8 /numgames 236".
   */
  public List<String> args;
  public int uid;
  public String mod;
  public String version;
}
