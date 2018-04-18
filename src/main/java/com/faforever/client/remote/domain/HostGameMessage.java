package com.faforever.client.remote.domain;

import com.faforever.client.game.GameVisibility;
import lombok.Getter;
import lombok.Setter;

/**
 * Data sent from the client to the FAF server to tell it about a preferences to be hosted.
 */
@Getter
@Setter
public class HostGameMessage extends ClientMessage {

  private String mapname;
  private String title;
  private String mod;
  private boolean[] options;
  private GameAccess access;
  private Integer version;
  private String password;
  private GameVisibility visibility;

  public HostGameMessage(GameAccess gameAccess, String mapName, String title, boolean[] options, String mod, String password, Integer version, GameVisibility gameVisibility) {
    super(ClientMessageType.HOST_GAME);
    access = gameAccess;
    this.mapname = mapName;
    this.title = title;
    this.options = options;
    this.mod = mod;
    this.password = password;
    this.version = version;
    this.visibility = gameVisibility;
  }

}
