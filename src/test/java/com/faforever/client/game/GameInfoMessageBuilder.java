package com.faforever.client.game;

import com.faforever.client.remote.domain.GameInfoMessage;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.VictoryCondition;

import java.util.ArrayList;
import java.util.HashMap;

public class GameInfoMessageBuilder {

  private GameInfoMessage gameInfoMessage;

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
    gameInfoMessage.setPasswordProtected(false);
    return this;
  }

  public GameInfoMessage get() {
    return gameInfoMessage;
  }

  public GameInfoMessageBuilder addTeamMember(String team, String playerName) {
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
