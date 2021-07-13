package com.faforever.client.remote.domain.inbound.gpg;


import com.fasterxml.jackson.annotation.JsonIgnore;

public class GpgHostGameMessage extends GpgInboundMessage {
  public static final String COMMAND = "HostGame";

  private static final int MAP_INDEX = 0;

  public GpgHostGameMessage() {
    super(1);
  }

  @JsonIgnore
  public String getMap() {
    return getArgAsString(MAP_INDEX);
  }

  @JsonIgnore
  public void setMap(String map) {
    setArgAsValue(MAP_INDEX, map);
  }
}
