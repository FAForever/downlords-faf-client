package com.faforever.client.fa;

import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.GameType;

import java.util.List;
import java.util.Map;

public record GameParameters(
    Integer uid,
    String name,
    String featuredMod,
    GameType gameType,
    String leaderboard,
    List<String> additionalArgs,
    String mapName,
    Integer expectedPlayers,
    Integer mapPosition,
    Map<String, String> gameOptions,
    Integer team,
    Faction faction,
    League league
) {

  public record League(String division, String subDivision) {}

}
