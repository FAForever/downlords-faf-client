package com.faforever.client.game;

import com.faforever.client.fa.relay.LobbyMode;
import com.faforever.client.remote.domain.GameLaunchMessage;

import java.util.Arrays;

import static java.util.Collections.emptyList;

public class GameLaunchMessageBuilder {

  private final GameLaunchMessage gameLaunchMessage;

  public GameLaunchMessageBuilder() {
    gameLaunchMessage = new GameLaunchMessage();
  }

  public static GameLaunchMessageBuilder create() {
    return new GameLaunchMessageBuilder();
  }

  public GameLaunchMessageBuilder defaultValues() {
    gameLaunchMessage.setUid(1);
    gameLaunchMessage.setMod(KnownFeaturedMod.DEFAULT.getTechnicalName());
    gameLaunchMessage.setArgs(emptyList());
    return this;
  }

  public GameLaunchMessage get() {
    return gameLaunchMessage;
  }

  public GameLaunchMessageBuilder uid(int uid) {
    gameLaunchMessage.setUid(uid);
    return this;
  }

  public GameLaunchMessageBuilder mod(String mod) {
    gameLaunchMessage.setMod(mod);
    return this;
  }

  public GameLaunchMessageBuilder expectedPlayers(int expectedPlayers) {
    gameLaunchMessage.setExpectedPlayers(expectedPlayers);
    return this;
  }

  public GameLaunchMessageBuilder team(int team) {
    gameLaunchMessage.setTeam(team);
    return this;
  }

  public GameLaunchMessageBuilder faction(Faction faction) {
    gameLaunchMessage.setFaction(faction);
    return this;
  }

  public GameLaunchMessageBuilder initMode(LobbyMode initMode) {
    gameLaunchMessage.setInitMode(initMode);
    return this;
  }

  public GameLaunchMessageBuilder mapPosition(int mapPosition) {
    gameLaunchMessage.setMapPosition(mapPosition);
    return this;
  }

  public GameLaunchMessageBuilder mapname(String mapname) {
    gameLaunchMessage.setMapname(mapname);
    return this;
  }

  public GameLaunchMessageBuilder args(String... args) {
    gameLaunchMessage.setArgs(Arrays.asList(args));
    return this;
  }
}
