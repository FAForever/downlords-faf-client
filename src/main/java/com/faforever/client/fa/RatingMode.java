package com.faforever.client.fa;

import com.faforever.client.game.KnownFeaturedMod;

public enum RatingMode {
  GLOBAL,
  LADDER_1V1,
  LADDER_2V2,
  NONE;

  public KnownFeaturedMod getCorrespondingFeatureMod() {
    switch (this) {
      case LADDER_1V1: return KnownFeaturedMod.LADDER_1V1;
      case LADDER_2V2: return KnownFeaturedMod.LADDER_2V2;
      default: throw new UnsupportedOperationException("FeatureMod for this rating mod is unsupported or not created yet");
    }
  }
}


