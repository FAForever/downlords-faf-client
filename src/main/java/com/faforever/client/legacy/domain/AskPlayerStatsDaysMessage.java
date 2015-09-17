package com.faforever.client.legacy.domain;

public class AskPlayerStatsDaysMessage extends ClientMessage {

  private String type;
  private String player;

  public AskPlayerStatsDaysMessage(String username, StatisticsType type) {
    setCommand(ClientMessageType.STATISTICS);
    this.setType(type.getString());
    setPlayer(username);
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getPlayer() {
    return player;
  }

  public void setPlayer(String player) {
    this.player = player;
  }
}
