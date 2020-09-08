package com.faforever.client.fa;

import com.faforever.client.game.KnownFeaturedMod;
import lombok.Getter;

public enum RatingMode {
  GLOBAL("userInfo.ratingHistory.global", KnownFeaturedMod.FAF),
  LADDER_1V1("userInfo.ratingHistory.1v1", KnownFeaturedMod.LADDER_1V1),
  NONE("", null);

  @Getter
  private final String i18nKey;
  @Getter
  private final KnownFeaturedMod featuredMod;

  RatingMode(String i18nKey, KnownFeaturedMod featuredMod) {
    this.i18nKey = i18nKey;
    this.featuredMod = featuredMod;
  }
}
