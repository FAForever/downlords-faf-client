package com.faforever.client.builders;

import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.LobbyMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameLaunchMessageBuilder {

  private List<String> args;
  private int uid;
  private String featuredMod;
  private String mapname;
  private String name;
  private Integer expectedPlayers;
  private Integer team;
  private Integer mapPosition;
  private Map<String, String> gameOptions;
  private Faction faction;
  @Deprecated
  private LobbyMode initMode;
  private GameType gameType;
  private String ratingType;

  public static GameLaunchMessageBuilder create() {
    return new GameLaunchMessageBuilder();
  }

  public GameLaunchMessageBuilder defaultValues() {
    name("test");
    uid(1);
    featuredMod(KnownFeaturedMod.DEFAULT.getTechnicalName());
    args();
    gameType(GameType.CUSTOM);
    ratingType("global");
    initMode(LobbyMode.DEFAULT_LOBBY);
    return this;
  }

  public GameLaunchResponse get() {
    return new GameLaunchResponse(uid, name, featuredMod, initMode, gameType, ratingType, args, mapname,
                                  expectedPlayers, mapPosition, gameOptions, team, faction);
  }

  public GameLaunchMessageBuilder name(String name) {
    this.name = name;
    return this;
  }

  public GameLaunchMessageBuilder uid(int uid) {
    this.uid = uid;
    return this;
  }

  public GameLaunchMessageBuilder featuredMod(String featuredMod) {
    this.featuredMod = featuredMod;
    return this;
  }

  public GameLaunchMessageBuilder expectedPlayers(int expectedPlayers) {
    this.expectedPlayers = expectedPlayers;
    return this;
  }

  public GameLaunchMessageBuilder team(int team) {
    this.team = team;
    return this;
  }

  public GameLaunchMessageBuilder faction(Faction faction) {
    this.faction = faction;
    return this;
  }

  public GameLaunchMessageBuilder ratingType(String ratingType) {
    this.ratingType = ratingType;
    return this;
  }

  @Deprecated
  public GameLaunchMessageBuilder initMode(LobbyMode initMode) {
    this.initMode = initMode;
    return this;
  }

  public GameLaunchMessageBuilder gameType(GameType gameType) {
    this.gameType = gameType;
    return this;
  }

  public GameLaunchMessageBuilder mapPosition(int mapPosition) {
    this.mapPosition = mapPosition;
    return this;
  }

  public GameLaunchMessageBuilder gameOptions(Map<String, String> gameOptions) {
    this.gameOptions = gameOptions;
    return this;
  }

  public GameLaunchMessageBuilder mapName(String mapname) {
    this.mapname = mapname;
    return this;
  }

  public GameLaunchMessageBuilder args(String... args) {
    this.args = new ArrayList<>(List.of(args));
    return this;
  }
}
