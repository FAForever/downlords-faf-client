package com.faforever.client.patch;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.game.KnownFeaturedMod;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Execute all necessary tasks such as downloading featured mod, patching the executable, downloading other sim mods and
 * generating the init file in order to put the preferences into a runnable state for a specific featured mod and version.
 */
public interface GameUpdater {

  /**
   * Adds an updater to the chain. For each mod to update, the first updater which can update a mod will be called.
   */
  GameUpdater addFeaturedModUpdater(FeaturedModUpdater featuredModUpdater);

  /**
   * @param featuredMod the featured "base" mod is the one onto which other mods base on (usually {@link
   * KnownFeaturedMod#DEFAULT}).
   * @param featuredModVersions a map of indices ("1","2","3","4"...) to version numbers. The indices represent the ID
   * of the featured mod as stored in the server's database.
   * @param simModUids a list of sim mod UIDs to update
   * @return a completion stage that, when completed, is called with a `mountpoint -> path` which can be used to
   * generate the FA ini file.
   */
  CompletableFuture<Void> update(FeaturedModBean featuredMod, Integer version, Map<String, Integer> featuredModVersions, Set<String> simModUids);
}
