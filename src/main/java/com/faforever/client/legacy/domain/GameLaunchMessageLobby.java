package com.faforever.client.legacy.domain;

import java.util.List;

public class GameLaunchMessageLobby extends FafServerMessage {

  private List<String> args;
  private int uid;
  private String mod;

  public GameLaunchMessageLobby() {
    super(FafServerMessageType.GAME_LAUNCH);
  }

  /**
   * Stores game launch arguments, like "/ratingcolor d8d8d8d8 /numgames 236".
   */
  public List<String> getArgs() {
    return args;
  }

  public void setArgs(List<String> args) {
    this.args = args;
  }

  public int getUid() {
    return uid;
  }

  public void setUid(int uid) {
    this.uid = uid;
  }

  public String getMod() {
    return mod;
  }

  public void setMod(String mod) {
    this.mod = mod;
  }
}
