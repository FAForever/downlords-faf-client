package com.faforever.client.legacy.domain;

public class AskPlayerStatsDaysMessage extends ClientMessage {

  public String type;
  public String player;

  public AskPlayerStatsDaysMessage(String username, StatisticsType type) {
    command = "stats";
    this.type = type.getString();
    player = username;
  }
}
