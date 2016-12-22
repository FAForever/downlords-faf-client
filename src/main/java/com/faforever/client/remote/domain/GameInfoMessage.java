package com.faforever.client.remote.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GameInfoMessage extends FafServerMessage {

  private String host;
  private Boolean passwordProtected;
  // TODO use enum
  private String visibility;
  private GameStatus state;
  private Integer numPlayers;
  private Map<String, List<String>> teams;
  private Map<String, Integer> featuredModVersions;
  private String featuredMod;
  private Integer uid;
  private Integer maxPlayers;
  private String title;
  // FAF calls this "game_type" but it's actually the victory condition.
  private VictoryCondition gameType;
  private Map<String, String> simMods;
  private String mapname;
  private Double launchedAt;
  /**
   * The server may either send a single game or a list of games in the same message... *cringe*.
   */
  private List<GameInfoMessage> games;

  public GameInfoMessage() {
    super(FafServerMessageType.GAME_INFO);
  }

  public List<GameInfoMessage> getGames() {
    return games;
  }

  public void setGames(List<GameInfoMessage> games) {
    this.games = games;
  }

  @Override
  public String toString() {
    return "GameInfo{" +
        "uid=" + uid +
        ", title='" + title + '\'' +
        ", state=" + state +
        '}';
  }
}
