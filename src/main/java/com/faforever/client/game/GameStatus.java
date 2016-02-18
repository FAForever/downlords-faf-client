package com.faforever.client.game;

import com.faforever.client.remote.domain.GameState;

public enum GameStatus {
  NONE, HOST, LOBBY, PLAYING;

  //FIXME cannot resolve gamestatus from gamestate
  public static GameStatus getFromGameState(GameState gameState) {
    switch (gameState.getString()) {
      case "playing":
        return GameStatus.PLAYING;
      case "open":
        return GameStatus.LOBBY;
      //unknown and closed
      default:
        return GameStatus.NONE;
    }
  }
}
