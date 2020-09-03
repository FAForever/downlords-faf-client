package com.faforever.client.remote.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString(of = {"uid", "title", "state"})
public class GameInfoMessage extends FafServerMessage {

  private String host;
  private Boolean passwordProtected;
  // TODO use enum
  private String visibility;
  private GameStatus state;
  private Integer numPlayers;
  private Map<String, List<String>> teams;
  private String featuredMod;
  private Integer uid;
  private Integer maxPlayers;
  private String title;
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
}
