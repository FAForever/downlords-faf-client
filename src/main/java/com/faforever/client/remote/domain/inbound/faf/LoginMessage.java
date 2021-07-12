package com.faforever.client.remote.domain.inbound.faf;

import com.faforever.client.remote.domain.PlayerInfo;
import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class LoginMessage extends FafInboundMessage {
  public static final String COMMAND = "welcome";

  PlayerInfo me;
}
