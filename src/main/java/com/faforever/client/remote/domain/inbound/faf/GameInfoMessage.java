package com.faforever.client.remote.domain.inbound.faf;

import com.faforever.client.game.GameVisibility;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import com.fasterxml.jackson.annotation.JsonProperty;
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
  @JsonProperty("password_protected")
  Boolean passwordProtected;
  GameVisibility visibility;
  @ToString.Include
  GameStatus state;
  @JsonProperty("num_players")
  Integer numPlayers;
  Map<String, List<String>> teams;
  @JsonProperty("featured_mod")
  String featuredMod;
  @ToString.Include
  Integer uid;
  @JsonProperty("max_players")
  Integer maxPlayers;
  @ToString.Include
  String title;
  @JsonProperty("sim_mods")
  Map<String, String> simMods;
  String mapname;
  @JsonProperty("launched_at")
  Double launchedAt;
  @JsonProperty("rating_type")
  String ratingType;
  @JsonProperty("rating_min")
  Integer ratingMin;
  @JsonProperty("rating_max")
  Integer ratingMax;
  @JsonProperty("enforce_rating_range")
  Boolean enforceRatingRange;
  @JsonProperty("game_type")
  GameType gameType;
  /**
   * The server may either send a single game or a list of games in the same message... *cringe*.
   */
  List<GameInfoMessage> games;
}
