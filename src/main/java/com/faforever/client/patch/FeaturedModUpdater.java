package com.faforever.client.patch;

import com.faforever.client.mod.FeaturedMod;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface FeaturedModUpdater {

  /**
   * Updates the specified featured mod to the specified version. If {@code version} is null, it will update to the
   * latest version
   */
  CompletableFuture<PatchResult> updateMod(FeaturedMod featuredMod, @Nullable Integer version);

  /**
   * Returns {@code true} if this updater is able to update the specified featured mod.
   */
  boolean canUpdate(FeaturedMod featuredMod);
}
