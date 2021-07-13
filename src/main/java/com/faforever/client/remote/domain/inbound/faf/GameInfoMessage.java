package com.faforever.client.remote.domain.inbound.faf;

import com.faforever.client.game.GameVisibility;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.GameType;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.List;
import java.util.Map;


@EqualsAndHashCode(callSuper = true)
@Value
@ToString(onlyExplicitlyIncluded = true)
public class GameInfoMessage extends FafInboundMessage {
  public static final String COMMAND = "game_info";

  String host;
  Boolean passwordProtected;
  GameVisibility visibility;
  @ToString.Include
  GameStatus state;
  Integer numPlayers;
  Map<String, List<String>> teams;
  String featuredMod;
  @ToString.Include
  Integer uid;
  Integer maxPlayers;
  @ToString.Include
  String title;
  Map<String, String> simMods;
  String mapname;
  Double launchedAt;
  String ratingType;
  Integer ratingMin;
  Integer ratingMax;
  Boolean enforceRatingRange;
  GameType gameType;
  /**
   * The server may either send a single game or a list of games in the same message... *cringe*.
   */
  List<GameInfoMessage> games;
}
