package com.faforever.client.game;

import lombok.Getter;

public enum PlayerGameStatus {

  IDLE("game.gameStatus.idle"),
  HOSTING("game.gameStatus.hosting"),
  LOBBYING("game.gameStatus.lobby"),
  PLAYING("game.gameStatus.playing");

  @Getter
  private final String i18nKey;

  PlayerGameStatus(String i18nKey) {
    this.i18nKey = i18nKey;
  }
}
