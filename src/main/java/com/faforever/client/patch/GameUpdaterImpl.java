package com.faforever.client.patch;

import com.faforever.client.game.FaInitGenerator;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import com.faforever.commons.mod.MountInfo;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.faforever.client.game.KnownFeaturedMod.BALANCE_TESTING;
import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static com.faforever.client.game.KnownFeaturedMod.FAF_BETA;
import static com.faforever.client.game.KnownFeaturedMod.FAF_DEVELOP;
import static com.faforever.client.game.KnownFeaturedMod.LADDER_1V1;

public class GameUpdaterImpl implements GameUpdater {

  private static final List<String> NAMES_OF_FEATURED_BASE_MODS = Stream.of(FAF, FAF_BETA, FAF_DEVELOP, BALANCE_TESTING, LADDER_1V1)
      .map(KnownFeaturedMod::getTechnicalName)
      .collect(Collectors.toList());

  private final List<FeaturedModUpdater> featuredModUpdaters;
  private final ModService modService;
  private final ApplicationContext applicationContext;
  private final TaskService taskService;
  private final FafService fafService;
  private final FaInitGenerator faInitGenerator;

  @Inject
  public GameUpdaterImpl(ModService modService, ApplicationContext applicationContext, TaskService taskService, FafService fafService, FaInitGenerator faInitGenerator) {
    featuredModUpdaters = new ArrayList<>();
    this.modService = modService;
    this.applicationContext = applicationContext;
    this.taskService = taskService;
    this.fafService = fafService;
    this.faInitGenerator = faInitGenerator;
  }

  @Override
  public GameUpdater addFeaturedModUpdater(FeaturedModUpdater featuredModUpdater) {
    featuredModUpdaters.add(featuredModUpdater);
    return this;
  }

  @Override
  public CompletableFuture<Void> update(FeaturedMod featuredMod, Integer version, Map<String, Integer> featuredModVersions, Set<String> simModUids) {
    // The following ugly code is sponsored by the featured-mod-mess. FAF and Coop are both featured mods - but others,
    // (except fafbeta and fafdevelop) implicitly depend on FAF. So if a non-base mod is being played, make sure FAF is
    // installed.
    List<PatchResult> patchResults = new ArrayList<>();

    CompletableFuture<Void> future = updateFeaturedMod(featuredMod, version)
        .thenAccept(patchResults::add)
        .thenAccept(aVoid -> downloadMissingSimMods(simModUids));

    if (!NAMES_OF_FEATURED_BASE_MODS.contains(featuredMod.getTechnicalName())) {
      future = future.thenCompose(aVoid -> modService.getFeaturedMod(FAF.getTechnicalName()))
          .thenCompose(baseMod -> updateFeaturedMod(baseMod, null))
          .thenAccept(patchResults::add);
    }

    future.thenCompose(aVoid -> updateGameBinaries(patchResults.get(patchResults.size() - 1).getVersion()))
        .thenRun(() -> {
          List<MountInfo> mountPoints = patchResults.stream()
              .flatMap(patchResult -> patchResult.getMountInfos().stream())
              .collect(Collectors.toList());

          Set<String> hookDirectories = patchResults.stream()
              .flatMap(patchResult -> patchResult.getHookDirectories().stream())
              .collect(Collectors.toSet());

          faInitGenerator.generateInitFile(mountPoints, hookDirectories);
        });

    return future;
  }

  @Override
  public CompletableFuture<List<FeaturedMod>> getFeaturedMods() {
    return fafService.getFeaturedMods();
  }

  private CompletableFuture<Void> downloadMissingSimMods(Set<String> simModUids) {
    if (simModUids == null || simModUids.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    List<CompletableFuture<Void>> simModFutures = simModUids.stream()
        .filter(uid -> !modService.isModInstalled(uid))
        .map(modService::downloadAndInstallMod)
        .collect(Collectors.toList());
    return CompletableFuture.allOf(simModFutures.toArray(new CompletableFuture[simModFutures.size()]));
  }

  private CompletableFuture<PatchResult> updateFeaturedMod(FeaturedMod featuredMod, Integer version) {
    for (FeaturedModUpdater featuredModUpdater : featuredModUpdaters) {
      if (featuredModUpdater.canUpdate(featuredMod)) {
        return featuredModUpdater.updateMod(featuredMod, version);
      }
    }
    throw new UnsupportedOperationException("No updater available for featured mod: " + featuredMod
        + " with version:" + version);
  }

  private CompletableFuture<Void> updateGameBinaries(ComparableVersion version) {
    GameBinariesUpdateTask binariesUpdateTask = applicationContext.getBean(GameBinariesUpdateTaskImpl.class);
    binariesUpdateTask.setVersion(version);
    return taskService.submitTask(binariesUpdateTask).getFuture();
  }
}
