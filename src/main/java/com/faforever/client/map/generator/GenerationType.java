package com.faforever.client.map.generator;

import lombok.Getter;

public enum GenerationType {
  CASUAL("game.generateMap.casual"),
  TOURNAMENT("game.generateMap.tournament"),
  BLIND("game.generateMap.blind");

  @Getter
  private final String i18nKey;

  GenerationType(String i18nKey) {
    this.i18nKey = i18nKey;
  }

}
