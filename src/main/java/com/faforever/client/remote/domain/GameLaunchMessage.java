package com.faforever.client.remote.domain;

import java.util.List;

public class GameLaunchMessage extends FafServerMessage {

  private List<String> args;
  private int uid;
  private String mod;
  private String mapname;

  public GameLaunchMessage() {
    super(FafServerMessageType.GAME_LAUNCH);
  }

  /**
   * Stores game launch arguments, like "/ratingcolor d8d8d8d8", "/numgames 236".
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

  public String getMapname() {
    return mapname;
  }

  public void setMapname(String mapname) {
    this.mapname = mapname;
  }
}
