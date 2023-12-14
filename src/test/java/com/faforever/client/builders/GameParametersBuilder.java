package com.faforever.client.builders;

import com.faforever.client.fa.GameParameters;
import com.faforever.client.fa.GameParameters.League;
import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.GameType;

import java.util.List;
import java.util.Map;

public class GameParametersBuilder {

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
  private League league;

  public static GameParametersBuilder create() {
    return new GameParametersBuilder();
  }

  public GameParametersBuilder defaultValues() {
    uid(1);
    featuredMod("faf");
    gameType(GameType.CUSTOM);
    leaderboard("global");
    return this;
  }

  public GameParametersBuilder uid(Integer uid) {
    this.uid = uid;
    return this;
  }

  public GameParametersBuilder name(String name) {
    this.name = name;
    return this;
  }

  public GameParametersBuilder featuredMod(String featuredMod) {
    this.featuredMod = featuredMod;
    return this;
  }

  public GameParametersBuilder gameType(GameType gameType) {
    this.gameType = gameType;
    return this;
  }

  public GameParametersBuilder leaderboard(String leaderboard) {
    this.leaderboard = leaderboard;
    return this;
  }

  public GameParametersBuilder additionalArgs(List<String> additionalArgs) {
    this.additionalArgs = additionalArgs;
    return this;
  }

  public GameParametersBuilder mapName(String mapName) {
    this.mapName = mapName;
    return this;
  }

  public GameParametersBuilder expectedPlayers(Integer expectedPlayers) {
    this.expectedPlayers = expectedPlayers;
    return this;
  }

  public GameParametersBuilder mapPosition(Integer mapPosition) {
    this.mapPosition = mapPosition;
    return this;
  }

  public GameParametersBuilder gameOptions(Map<String, String> gameOptions) {
    this.gameOptions = gameOptions;
    return this;
  }

  public GameParametersBuilder team(Integer team) {
    this.team = team;
    return this;
  }

  public GameParametersBuilder faction(Faction faction) {
    this.faction = faction;
    return this;
  }

  public GameParametersBuilder league(League league) {
    this.league = league;
    return this;
  }

  public GameParameters get() {
    return new GameParameters(uid, name, featuredMod, gameType, leaderboard, additionalArgs, mapName, expectedPlayers,
                              mapPosition, gameOptions, team, faction, league);
  }


}
