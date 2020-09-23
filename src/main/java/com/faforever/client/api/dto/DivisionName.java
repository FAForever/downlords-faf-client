package com.faforever.client.api.dto;

import lombok.Getter;

public enum DivisionName {
  BRONZE("leagues.divisionName.bronze"),
  SILVER("leagues.divisionName.silver"),
  GOLD("leagues.divisionName.gold"),
  DIAMOND("leagues.divisionName.diamond"),
  MASTER("leagues.divisionName.master"),
  COMMANDER("leagues.divisionName.commander"),
  I("leagues.divisionName.I"),
  II("leagues.divisionName.II"),
  III("leagues.divisionName.III"),
  IV("leagues.divisionName.IV"),
  V("leagues.divisionName.V"),
  NONE("leagues.divisionName.none");

  @Getter
  private final String i18nKey;

  DivisionName(String i18nKey) {
    this.i18nKey = i18nKey;
  }
}
