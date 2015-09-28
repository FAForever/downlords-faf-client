package com.faforever.client.game;

import com.faforever.client.legacy.LobbyServerAccessor;
import org.springframework.beans.factory.annotation.Autowired;

public class SearchExpansionTask implements Runnable {

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;
  private float maxRadius;
  private float radius;
  private float radiusIncrement;

  @Override
  public void run() {
    if (radius < maxRadius) {
      radius += radiusIncrement;
    }

    lobbyServerAccessor.expand1v1Search(radius);
  }

  public void setMaxRadius(float maxRadius) {
    this.maxRadius = maxRadius;
  }

  public void setRadiusIncrement(float radiusIncrement) {
    this.radiusIncrement = radiusIncrement;
  }
}
