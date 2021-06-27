package com.faforever.client.game;

import com.faforever.client.remote.domain.LobbyMode;
import com.faforever.client.remote.domain.inbound.faf.GameLaunchMessage;
import com.faforever.commons.api.dto.Faction;

import java.util.ArrayList;
import java.util.List;

public class GameLaunchMessageTestBuilder {

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

  public static GameLaunchMessageTestBuilder create() {
    return new GameLaunchMessageTestBuilder();
  }

  public GameLaunchMessageTestBuilder defaultValues() {
    name("test");
    uid(1);
    mod(KnownFeaturedMod.DEFAULT.getTechnicalName());
    args();
    ratingType("global");
    return this;
  }

  public GameLaunchMessage get() {
    return new GameLaunchMessage(args, uid, mod, mapname, name, expectedPlayers, team, mapPosition, faction, initMode,
        ratingType);
  }

  public GameLaunchMessageTestBuilder name(String name) {
    this.name = name;
    return this;
  }

  public GameLaunchMessageTestBuilder uid(int uid) {
    this.uid = uid;
    return this;
  }

  public GameLaunchMessageTestBuilder mod(String mod) {
    this.mod = mod;
    return this;
  }

  public GameLaunchMessageTestBuilder expectedPlayers(int expectedPlayers) {
    this.expectedPlayers = expectedPlayers;
    return this;
  }

  public GameLaunchMessageTestBuilder team(int team) {
    this.team = team;
    return this;
  }

  public GameLaunchMessageTestBuilder faction(Faction faction) {
    this.faction = faction;
    return this;
  }

  public GameLaunchMessageTestBuilder ratingType(String ratingType) {
    this.ratingType = ratingType;
    return this;
  }

  public GameLaunchMessageTestBuilder initMode(LobbyMode initMode) {
    this.initMode = initMode;
    return this;
  }

  public GameLaunchMessageTestBuilder mapPosition(int mapPosition) {
    this.mapPosition = mapPosition;
    return this;
  }

  public GameLaunchMessageTestBuilder mapname(String mapname) {
    this.mapname = mapname;
    return this;
  }

  public GameLaunchMessageTestBuilder args(String... args) {
    this.args = new ArrayList<>(List.of(args));
    return this;
  }
}
