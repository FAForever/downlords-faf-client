package com.faforever.client.remote.domain.inbound.faf;

import com.faforever.client.remote.domain.LobbyMode;
import com.faforever.commons.api.dto.Faction;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Value
public class GameLaunchMessage extends FafInboundMessage {
  public static final String COMMAND = "game_launch";

  /**
   * Stores game launch arguments, like "/ratingcolor d8d8d8d8", "/numgames 236".
   */
  List<String> args;
  int uid;
  String mod;
  String mapname;
  String name;
  Integer expectedPlayers;
  Integer team;
  Integer mapPosition;
  Faction faction;
  LobbyMode initMode;
  String ratingType;
}
