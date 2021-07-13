package com.faforever.client.remote.domain.outbound.faf;


import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class JoinGameMessage extends FafOutboundMessage {
  public static final String COMMAND = "game_join";

  Integer uid;
  String password;
}
