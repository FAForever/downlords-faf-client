package com.faforever.client.remote.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
public class BanPlayerMessage extends ClientMessage {
  final private int user_id;
  final private String action = "closelobby";
  final private Ban ban;

  public BanPlayerMessage(int user_id, String reason, int duration, String period) {
    super(ClientMessageType.ADMIN);
    this.user_id = user_id;
    this.ban = new Ban(reason, duration, period);
  }

  @Data
  @AllArgsConstructor
  private class Ban {
    private String reason;
    private int duration;
    private String period;
  }
}
