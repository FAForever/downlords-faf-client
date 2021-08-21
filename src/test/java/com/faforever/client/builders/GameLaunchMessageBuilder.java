package com.faforever.client.builders;

import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.LobbyMode;

import java.util.ArrayList;
import java.util.List;

public class GameLaunchMessageBuilder {

  private List<String> args;
  private int uid;
  private String mod;
  private String mapname;
  private String name;
  private Integer expectedPlayers;
  private Integer team;
  private Integer mapPosition;
  private Faction faction;
  private LobbyMode initMode;
  private String ratingType;

  public static GameLaunchMessageBuilder create() {
    return new GameLaunchMessageBuilder();
  }

  public GameLaunchMessageBuilder defaultValues() {
    name("test");
    uid(1);
    mod(KnownFeaturedMod.DEFAULT.getTechnicalName());
    args();
    ratingType("global");
    initMode(LobbyMode.DEFAULT_LOBBY);
    return this;
  }

  public GameLaunchResponse get() {
    return new GameLaunchResponse(uid, name, mod, initMode, ratingType, args, mapname, expectedPlayers, mapPosition, team, faction);
  }

  public GameLaunchMessageBuilder name(String name) {
    this.name = name;
    return this;
  }

  public GameLaunchMessageBuilder uid(int uid) {
    this.uid = uid;
    return this;
  }

  public GameLaunchMessageBuilder mod(String mod) {
    this.mod = mod;
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

  public GameLaunchMessageBuilder initMode(LobbyMode initMode) {
    this.initMode = initMode;
    return this;
  }

  public GameLaunchMessageBuilder mapPosition(int mapPosition) {
    this.mapPosition = mapPosition;
    return this;
  }

  public GameLaunchMessageBuilder mapname(String mapname) {
    this.mapname = mapname;
    return this;
  }

  public GameLaunchMessageBuilder args(String... args) {
    this.args = new ArrayList<>(List.of(args));
    return this;
  }
}
