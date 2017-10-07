package com.faforever.client.update;

import com.faforever.client.FafClientApplication;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.patch.FeaturedModUpdater;
import com.faforever.client.patch.PatchResult;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;


@Lazy
@Component
@Profile(FafClientApplication.PROFILE_OFFLINE)
public class MockFeaturedModUpdater implements FeaturedModUpdater {

  @Override
  public CompletableFuture<PatchResult> updateMod(FeaturedMod featuredMod, @Nullable Integer version) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public boolean canUpdate(FeaturedMod featuredMod) {
    return true;
  }
}
