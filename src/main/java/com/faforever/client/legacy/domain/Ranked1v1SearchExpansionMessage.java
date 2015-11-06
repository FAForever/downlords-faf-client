package com.faforever.client.legacy.domain;

public class Ranked1v1SearchExpansionMessage extends ClientMessage {

  private String state;
  private String mod;
  private float rate;

  public Ranked1v1SearchExpansionMessage(float rate) {
    super(ClientMessageType.GAME_MATCH_MAKING);
    this.rate = rate;
    this.state = "expand";
    this.mod = "ladder1v1";
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getMod() {
    return mod;
  }

  public void setMod(String mod) {
    this.mod = mod;
  }

  public float getRate() {
    return rate;
  }

  public void setRate(float rate) {
    this.rate = rate;
  }
}
