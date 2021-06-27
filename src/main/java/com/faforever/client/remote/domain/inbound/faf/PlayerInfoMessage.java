package com.faforever.client.remote.domain.inbound.faf;

import com.faforever.client.remote.domain.PlayerInfo;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Value
public class PlayerInfoMessage extends FafInboundMessage {
  public static final String COMMAND = "player_info";

  List<PlayerInfo> players;
}
