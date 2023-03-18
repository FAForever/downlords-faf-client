package com.faforever.client.patch;

import com.faforever.client.domain.FeaturedModBean;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Execute all necessary tasks such as downloading featured mod, patching the executable, downloading other sim mods and
 * generating the init file in order to put the preferences into a runnable state for a specific featured mod and version.
 */
public interface GameUpdater {

  /**
   * Sets the updater. This needs to be called before calling update().
   */
  GameUpdater setFeaturedModUpdater(FeaturedModUpdater featuredModUpdater);

  /**
   * @param featuredMod the featured mod to update
   * @param baseVersion the version of faf to update to
   * @param simModUIDs a list of sim mod UIDs to update
   * @param featuredModFileVersions a map of the featuredModFileIds to the version of the file
   * @param useReplayFolder whether to update the files in the replay or the game path
   * @return a completion stage that completes when the mod is fully updated
   */
  CompletableFuture<Void> update(FeaturedModBean featuredMod, Set<String> simModUIDs,
                                 @Nullable Map<String, Integer> featuredModFileVersions, @Nullable Integer baseVersion,
                                 boolean useReplayFolder);
}
