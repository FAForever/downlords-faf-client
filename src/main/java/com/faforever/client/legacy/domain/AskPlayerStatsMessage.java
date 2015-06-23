package com.faforever.client.legacy.domain;

public class AskPlayerStatsMessage extends ClientMessage {

  public String type;
  public String player;

  public AskPlayerStatsMessage(String username) {
    command = "stats";
    type = "global_90_days";
    player = username;
  }
}
