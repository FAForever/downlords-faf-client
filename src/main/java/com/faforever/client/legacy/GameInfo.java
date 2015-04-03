package com.faforever.client.legacy;

import com.faforever.client.legacy.message.ServerMessage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class GameInfo extends ServerMessage {

  public Map<String, Integer> featuredModVersions;
  public String mapname;
  public Integer numPlayers;
  public BigDecimal gameTime;
  public Integer uid;
  public String title;
  public Map<String, String> simMods;
  public GameType gameType;
  public String host;
  public Map<String, List<String>> teams;
  public String access;
  public GameState state;
  public String featuredMod;
  public Integer maxPlayers;
  public Boolean[] options;
  public Integer minRanking;
  public Integer maxRanking;
}
