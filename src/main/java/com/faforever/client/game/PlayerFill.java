package com.faforever.client.game;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.jetbrains.annotations.NotNull;

public class PlayerFill implements Comparable<PlayerFill> {

  private IntegerProperty players;
  private IntegerProperty maxPlayers;

  public PlayerFill(Integer players, Integer maxPlayers) {
    this.players = new SimpleIntegerProperty(players);
    this.maxPlayers = new SimpleIntegerProperty(maxPlayers);
  }

  public IntegerProperty playersProperty() {
    return players;
  }

  public IntegerProperty maxPlayersProperty() {
    return maxPlayers;
  }

  @Override
  public int compareTo(@NotNull PlayerFill other) {
    if (getPlayers() == other.getPlayers()) {
      return Integer.compare(getMaxPlayers(), other.getMaxPlayers());
    }

    return Integer.compare(getPlayers(), other.getPlayers());
  }

  public int getPlayers() {
    return players.get();
  }

  public void setPlayers(int players) {
    this.players.set(players);
  }

  public int getMaxPlayers() {
    return maxPlayers.get();
  }

  public void setMaxPlayers(int maxPlayers) {
    this.maxPlayers.set(maxPlayers);
  }
}
