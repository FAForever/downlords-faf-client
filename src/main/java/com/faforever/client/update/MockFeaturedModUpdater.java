package com.faforever.client.update;

import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.patch.FeaturedModUpdater;
import com.faforever.client.patch.PatchResult;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MockFeaturedModUpdater implements FeaturedModUpdater {

  @Override
  public CompletionStage<PatchResult> updateMod(FeaturedModBean featuredMod, @Nullable Integer version) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public boolean canUpdate(FeaturedModBean featuredMod) {
    return true;
  }
}
