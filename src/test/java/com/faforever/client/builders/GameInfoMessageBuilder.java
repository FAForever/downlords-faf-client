package com.faforever.client.builders;

import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.commons.lobby.GameInfo;
import com.faforever.commons.lobby.GameStatus;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.GameVisibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameInfoMessageBuilder {

  private final Integer uid;
  private String host;
  private Boolean passwordProtected;
  private GameVisibility visibility;
  private GameStatus state;
  private Integer numPlayers;
  private Map<String, List<String>> teams;
  private String featuredMod;
  private Integer maxPlayers;
  private String title;
  private Map<String, String> simMods;
  private String mapname;
  private Double launchedAt;
  private String ratingType;
  private Integer ratingMin;
  private Integer ratingMax;
  private Boolean enforceRatingRange;
  private GameType gameType;
  private List<GameInfo> games;

  private GameInfoMessageBuilder(Integer uid) {
    this.uid = uid;
  }

  public static GameInfoMessageBuilder create(Integer uid) {
    return new GameInfoMessageBuilder(uid);
  }

  public GameInfo get() {
    return new GameInfo(uid, title, host, gameType, maxPlayers, numPlayers, visibility, passwordProtected, state,
        featuredMod, ratingType, simMods, mapname, mapname, launchedAt, teams, ratingMin, ratingMax,
        enforceRatingRange, games);
  }

  public GameInfoMessageBuilder defaultValues() {
    host("Some host");
    featuredMod(KnownFeaturedMod.FAF.getTechnicalName());
    mapName("scmp_007");
    maxPlayers(4);
    numPlayers(1);
    state(GameStatus.OPEN);
    title("Test preferences");
    teams(new HashMap<>());
    passwordProtected(false);
    enforceRatingRange(false);
    ratingMax(3000);
    ratingMin(0);
    return this;
  }

  public GameInfoMessageBuilder addTeamMember(String team, String playerName) {
    if (!teams.containsKey(team)) {
      teams.put(team, new ArrayList<>());
    }
    teams.get(team).add(playerName);
    return this;
  }

  public GameInfoMessageBuilder teams(Map<String, List<String>> teams) {
    this.teams = teams;
    return this;
  }

  public GameInfoMessageBuilder simMods(Map<String, String> simMods) {
    this.simMods = simMods;
    return this;
  }

  public GameInfoMessageBuilder host(String host) {
    this.host = host;
    return this;
  }

  public GameInfoMessageBuilder title(String title) {
    this.title = title;
    return this;
  }

  public GameInfoMessageBuilder mapName(String mapName) {
    this.mapname = mapName;
    return this;
  }

  public GameInfoMessageBuilder launchedAt(Double launchedAt) {
    this.launchedAt = launchedAt;
    return this;
  }

  public GameInfoMessageBuilder ratingType(String ratingType) {
    this.ratingType = ratingType;
    return this;
  }

  public GameInfoMessageBuilder gameType(GameType gameType) {
    this.gameType = gameType;
    return this;
  }

  public GameInfoMessageBuilder games(List<GameInfo> games) {
    this.games = games;
    return this;
  }

  public GameInfoMessageBuilder visibility(GameVisibility visibility) {
    this.visibility = visibility;
    return this;
  }

  public GameInfoMessageBuilder featuredMod(String mod) {
    this.featuredMod = mod;
    return this;
  }

  public GameInfoMessageBuilder numPlayers(int numPlayers) {
    this.numPlayers = numPlayers;
    return this;
  }

  public GameInfoMessageBuilder maxPlayers(int maxPlayers) {
    this.maxPlayers = maxPlayers;
    return this;
  }

  public GameInfoMessageBuilder state(GameStatus state) {
    this.state = state;
    return this;
  }

  public GameInfoMessageBuilder passwordProtected(boolean passwordProtected) {
    this.passwordProtected = passwordProtected;
    return this;
  }

  public GameInfoMessageBuilder enforceRatingRange(boolean enforceRatingRange) {
    this.enforceRatingRange = enforceRatingRange;
    return this;
  }

  public GameInfoMessageBuilder ratingMax(Integer ratingMax) {
    this.ratingMax = ratingMax;
    return this;
  }

  public GameInfoMessageBuilder ratingMin(Integer ratingMin) {
    this.ratingMin = ratingMin;
    return this;
  }
}
