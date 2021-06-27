package com.faforever.client.remote.domain.outbound.faf;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class ClosePlayersFAMessage extends AdminMessage {
  int userId;


  public ClosePlayersFAMessage(int userId) {
    super("closeFA");
    this.userId = userId;
  }
}