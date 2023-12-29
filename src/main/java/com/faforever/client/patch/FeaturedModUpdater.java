package com.faforever.client.patch;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface FeaturedModUpdater {

  /**
   * Updates the specified featured mod to the specified version. If {@code version} is null, it will update to the
   * latest version
   */
  CompletableFuture<PatchResult> updateMod(String featuredModName, @Nullable Integer version, boolean useReplayFolder);

}
