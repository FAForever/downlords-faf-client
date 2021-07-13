package com.faforever.client.remote.domain.outbound.faf;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class AvatarMessage extends FafOutboundMessage {
  public static final String COMMAND = "avatar";

  private final String action;
}
