package com.faforever.client.remote.domain.outbound.faf;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class MakeBroadcastMessage extends AdminMessage {
  String message;


  public MakeBroadcastMessage(String message) {
    super("broadcast");
    this.message = message;
  }
}