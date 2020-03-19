package com.faforever.client.remote.domain;

import com.faforever.client.fa.relay.LobbyMode;
import com.faforever.client.game.Faction;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
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
  private Integer expectedPlayers;
  private Integer team;
  private Integer mapPosition;
  private Faction faction;
  private LobbyMode initMode;

  public GameLaunchMessage() {
    super(FafServerMessageType.GAME_LAUNCH);
  }
}
