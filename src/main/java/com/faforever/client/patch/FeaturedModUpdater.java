package com.faforever.client.patch;

import com.faforever.client.mod.FeaturedModBean;

import javax.annotation.Nullable;
import java.util.concurrent.CompletionStage;

public interface FeaturedModUpdater {

  /**
   * Updates the specified featured mod to the specified version. If {@code version} is null, it will update to the
   * latest version
   */
  CompletionStage<PatchResult> updateMod(FeaturedModBean featuredMod, @Nullable Integer version);

  /**
   * Returns {@code true} if this updater is able to update the specified featured mod.
   */
  boolean canUpdate(FeaturedModBean featuredMod);
}
