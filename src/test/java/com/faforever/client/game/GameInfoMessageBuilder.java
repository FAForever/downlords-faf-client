package com.faforever.client.game;

import com.faforever.client.remote.domain.GameInfoMessage;
import com.faforever.client.remote.domain.GameStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameInfoMessageBuilder {

  private final GameInfoMessage gameInfoMessage;

  private GameInfoMessageBuilder(Integer uid) {
    gameInfoMessage = new GameInfoMessage();
    gameInfoMessage.setUid(uid);
  }

  public static GameInfoMessageBuilder create(Integer uid) {
    return new GameInfoMessageBuilder(uid);
  }

  public GameInfoMessageBuilder defaultValues() {
    gameInfoMessage.setHost("Some host");
    gameInfoMessage.setFeaturedMod(KnownFeaturedMod.FAF.getTechnicalName());
    gameInfoMessage.setMapname("scmp_007");
    gameInfoMessage.setMaxPlayers(4);
    gameInfoMessage.setNumPlayers(1);
    gameInfoMessage.setState(GameStatus.OPEN);
    gameInfoMessage.setTitle("Test preferences");
    gameInfoMessage.setTeams(new HashMap<>());
    gameInfoMessage.setSimMods(new HashMap<>());
    gameInfoMessage.setPasswordProtected(false);
    gameInfoMessage.setEnforceRatingRange(false);
    gameInfoMessage.setRatingMax(3000);
    gameInfoMessage.setRatingMin(0);
    return this;
  }

  public GameInfoMessage get() {
    return gameInfoMessage;
  }

  public GameInfoMessageBuilder addTeamMember(String team, String playerName) {
    Map<String, List<String>> newTeams = new HashMap<>();
    if (!gameInfoMessage.getTeams().containsKey(team)) {
      gameInfoMessage.getTeams().put(team, new ArrayList<>());
    }
    gameInfoMessage.getTeams().get(team).add(playerName);
    return this;
  }

  public GameInfoMessageBuilder host(String host) {
    gameInfoMessage.setHost(host);
    return this;
  }

  public GameInfoMessageBuilder title(String title) {
    gameInfoMessage.setTitle(title);
    return this;
  }

  public GameInfoMessageBuilder mapName(String mapName) {
    gameInfoMessage.setMapname(mapName);
    return this;
  }

  public GameInfoMessageBuilder featuredMod(String mod) {
    gameInfoMessage.setFeaturedMod(mod);
    return this;
  }

  public GameInfoMessageBuilder numPlayers(int numPlayers) {
    gameInfoMessage.setNumPlayers(numPlayers);
    return this;
  }

  public GameInfoMessageBuilder maxPlayers(int maxPlayers) {
    gameInfoMessage.setMaxPlayers(maxPlayers);
    return this;
  }

  public GameInfoMessageBuilder state(GameStatus state) {
    gameInfoMessage.setState(state);
    return this;
  }

  public GameInfoMessageBuilder passwordProtected(boolean passwordProtected) {
    gameInfoMessage.setPasswordProtected(passwordProtected);
    return this;
  }
}
