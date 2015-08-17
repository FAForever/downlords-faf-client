package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.GameState;

public enum GameStatus {
  NONE, HOST, LOBBY, PLAYING;

  //FIXME cannot resolve gamestatus from gamestate
  public static GameStatus getFromGameState(GameState gameState) {
    switch (gameState.getString()) {
      case "playing":
        return GameStatus.PLAYING;
      case "open":
        return GameStatus.LOBBY;
    }
    return GameStatus.NONE;
  }
}
