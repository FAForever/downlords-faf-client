package com.faforever.client.patch;

import com.faforever.client.domain.FeaturedModBean;

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
   * @param featuredMod the featured mod to update
   * @param version the version of the featured mod to update to
   * @param simModUIDs a list of sim mod UIDs to update
   * @return a completion stage that completes when the mod is fully updated
   */
  CompletableFuture<Void> update(FeaturedModBean featuredMod, Integer version, Set<String> simModUIDs);
}
