package com.faforever.client.api.dto;

import lombok.Getter;

public enum Validity {
  // Order is crucial
  VALID("game.valid"),
  TOO_MANY_DESYNCS("game.reasonNotValid.desync"),
  WRONG_VICTORY_CONDITION("game.reasonNotValid.wrongCondition"),
  NO_FOG_OF_WAR("game.reasonNotValid.fogOfWar"),
  CHEATS_ENABLED("game.reasonNotValid.cheats"),
  PREBUILT_ENABLED("game.reasonNotValid.prebuilt"),
  NORUSH_ENABLED("game.reasonNotValid.noRush"),
  BAD_UNIT_RESTRICTIONS("game.reasonNotValid.unitRestriction"),
  BAD_MAP("game.reasonNotValid.unrankedMap"),
  TOO_SHORT("game.reasonNotValid.short"),
  BAD_MOD("game.reasonNotValid.unrankedMod"),
  COOP_NOT_RANKED("game.reasonNotValid.coop"),
  MUTUAL_DRAW("game.reasonNotValid.draw"),
  SINGLE_PLAYER("game.reasonNotValid.singlePlayer"),
  FFA_NOT_RANKED("game.reasonNotValid.ffa"),
  UNEVEN_TEAMS_NOT_RANKED("game.reasonNotValid.unevenTeams"),
  UNKNOWN_RESULT("game.reasonNotValid.unknown"),
  TEAMS_UNLOCKED("game.reasonNotValid.teamsUnlocked"),
  MULTIPLE_TEAMS("game.reasonNotValid.multipleTeams"),
  HAS_AI("game.reasonNotValid.ai"),
  CIVILIANS_REVEALED("game.reasonNotValid.civiliansRevealed"),
  WRONG_DIFFICULTY("game.reasonNotValid.difficulty"),
  EXPANSION_DISABLED("game.reasonNotValid.expansion"),
  SPAWN_NOT_FIXED("game.reasonNotValid.spawn"),
  OTHER_UNRANK("game.reasonNotValid.other");

  @Getter
  private final String i18nKey;

  Validity(String i18nKey) {
    this.i18nKey = i18nKey;
  }
}
