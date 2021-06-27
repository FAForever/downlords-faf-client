package com.faforever.client.remote.domain.outbound.faf;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class AdminMessage extends FafOutboundMessage {
  public static final String COMMAND = "admin";

  private final String action;
}
