package com.faforever.client.update;

import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.patch.FeaturedModUpdater;
import com.faforever.client.patch.PatchResult;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


@Lazy
@Component
@Profile("local")
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
