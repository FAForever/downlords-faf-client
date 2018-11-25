package com.faforever.client.remote.domain;

import com.faforever.client.game.Faction;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
public class GameLaunchMessage extends FafServerMessage {

  /**
   * Stores game launch arguments, like "/ratingcolor d8d8d8d8", "/numgames 236".
   */
  private List<String> args;
  private int uid;
  private String mod;
  private String mapname;
  @NonNull
  private String name;
  private int expectedPlayers;
  private int team;
  private int mapPosition;
  private Faction faction;

  public GameLaunchMessage() {
    super(FafServerMessageType.GAME_LAUNCH);
  }
}
