package com.faforever.client.legacy.message;

import java.util.List;

public class GameLaunchMessage extends ServerMessage {

  /**
   * Stores game launch arguments, like "/ratingcolor d8d8d8d8 /numgames 236".
   */
  private List<String> args;
  private String version;
  private int uid;
  private String mod;


  public String getVersion() {
    return version;
  }

  public int getUid() {
    return uid;
  }

  public String getMod() {
    return mod;
  }

  public List<String> getArgs() {
    return args;
  }
}
