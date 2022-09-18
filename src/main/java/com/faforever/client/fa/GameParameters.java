package com.faforever.client.fa;

import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.GameType;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GameParameters {
  private Integer uid;
  private String name;
  private String featuredMod;
  private GameType gameType;
  private String leaderboard;
  private List<String> additionalArgs;
  private String mapName;
  private Integer expectedPlayers;
  private Integer mapPosition;
  private Map<String, String> gameOptions;
  private Integer team;
  private Faction faction;

  private String division;
  private String subdivision;
  private Integer localGpgPort;
  private Integer localReplayPort;
  private boolean rehost;
}
