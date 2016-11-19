package com.faforever.client.patch;

import com.faforever.client.game.FeaturedModBean;
import com.faforever.client.game.KnownFeaturedMod;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface GameUpdateService {

  /**
   * @param featuredBaseMod the featured "base" mod is the one onto which other mods base on (usually {@link
   * KnownFeaturedMod#DEFAULT}).
   * @param featuredModVersions a map of indices ("1","2","3","4"...) to version numbers. The indices represent the ID
   * of the featured mod as stored in the server's database.
   * @param simModUids a list of sim mod UIDs to update
   */
  CompletionStage<Void> updateBaseMod(FeaturedModBean featuredBaseMod, Integer version, Map<String, Integer> featuredModVersions, Set<String> simModUids);
}
