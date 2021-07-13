package com.faforever.client.remote.domain.outbound.faf;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class BanPlayerMessage extends AdminMessage {
  int userId;
  Ban ban;


  public BanPlayerMessage(int userId, String reason, int duration, String period) {
    super("closelobby");
    this.userId = userId;
    this.ban = new Ban(reason, duration, period);
  }

  @Value
  private static class Ban {
    String reason;
    int duration;
    String period;
  }
}
