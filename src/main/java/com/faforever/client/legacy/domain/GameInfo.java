package com.faforever.client.legacy.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class GameInfo extends ServerObject {

  public String host;
  public Integer uid;
  public String title;
  public GameAccess access;
  public String mapname;
  public GameState state;
  public Boolean[] options;
  public GameType gameType;
  public String featuredMod;
  public Integer maxPlayers;
  public Integer minRanking;
  public Integer maxRanking;
  public Integer numPlayers;
  public BigDecimal gameTime;
  public Map<String, String> simMods;
  public Map<String, List<String>> teams;
  public Map<String, Integer> featuredModVersions;
}
