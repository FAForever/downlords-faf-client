package com.faforever.client.game;

import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.GameType;
import com.faforever.client.remote.domain.inbound.faf.GameInfoMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameInfoMessageTestBuilder {

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
  private List<GameInfoMessage> games;

  private GameInfoMessageTestBuilder(Integer uid) {
    this.uid = uid;
  }

  public static GameInfoMessageTestBuilder create(Integer uid) {
    return new GameInfoMessageTestBuilder(uid);
  }

  public GameInfoMessage get() {
    return new GameInfoMessage(host, passwordProtected, visibility, state, numPlayers, teams, featuredMod, uid, maxPlayers,
        title, simMods, mapname, launchedAt, ratingType, ratingMin, ratingMax, enforceRatingRange, gameType, games);
  }

  public GameInfoMessageTestBuilder defaultValues() {
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

  public GameInfoMessageTestBuilder addTeamMember(String team, String playerName) {
    if (!teams.containsKey(team)) {
      teams.put(team, new ArrayList<>());
    }
    teams.get(team).add(playerName);
    return this;
  }

  public GameInfoMessageTestBuilder teams(Map<String, List<String>> teams) {
    this.teams = teams;
    return this;
  }

  public GameInfoMessageTestBuilder simMods(Map<String, String> simMods) {
    this.simMods = simMods;
    return this;
  }

  public GameInfoMessageTestBuilder host(String host) {
    this.host = host;
    return this;
  }

  public GameInfoMessageTestBuilder title(String title) {
    this.title = title;
    return this;
  }

  public GameInfoMessageTestBuilder mapName(String mapName) {
    this.mapname = mapName;
    return this;
  }

  public GameInfoMessageTestBuilder launchedAt(Double launchedAt) {
    this.launchedAt = launchedAt;
    return this;
  }

  public GameInfoMessageTestBuilder ratingType(String ratingType) {
    this.ratingType = ratingType;
    return this;
  }

  public GameInfoMessageTestBuilder gameType(GameType gameType) {
    this.gameType = gameType;
    return this;
  }

  public GameInfoMessageTestBuilder games(List<GameInfoMessage> games) {
    this.games = games;
    return this;
  }

  public GameInfoMessageTestBuilder visibility(GameVisibility visibility) {
    this.visibility = visibility;
    return this;
  }

  public GameInfoMessageTestBuilder featuredMod(String mod) {
    this.featuredMod = mod;
    return this;
  }

  public GameInfoMessageTestBuilder numPlayers(int numPlayers) {
    this.numPlayers = numPlayers;
    return this;
  }

  public GameInfoMessageTestBuilder maxPlayers(int maxPlayers) {
    this.maxPlayers = maxPlayers;
    return this;
  }

  public GameInfoMessageTestBuilder state(GameStatus state) {
    this.state = state;
    return this;
  }

  public GameInfoMessageTestBuilder passwordProtected(boolean passwordProtected) {
    this.passwordProtected = passwordProtected;
    return this;
  }

  public GameInfoMessageTestBuilder enforceRatingRange(boolean enforceRatingRange) {
    this.enforceRatingRange = enforceRatingRange;
    return this;
  }

  public GameInfoMessageTestBuilder ratingMax(Integer ratingMax) {
    this.ratingMax = ratingMax;
    return this;
  }

  public GameInfoMessageTestBuilder ratingMin(Integer ratingMin) {
    this.ratingMin = ratingMin;
    return this;
  }
}
