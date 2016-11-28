package com.faforever.client.patch;

import com.faforever.client.game.FaInitGenerator;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.faforever.client.game.KnownFeaturedMod.BALANCE_TESTING;
import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static com.faforever.client.game.KnownFeaturedMod.FAF_BETA;
import static com.faforever.client.game.KnownFeaturedMod.FAF_DEVELOP;
import static com.faforever.client.game.KnownFeaturedMod.LADDER_1V1;

public class GameUpdaterImpl implements GameUpdater {

  private static final List<String> NAMES_OF_FEATURED_BASE_MODS = Stream.of(FAF, FAF_BETA, FAF_DEVELOP, BALANCE_TESTING, LADDER_1V1)
      .map(KnownFeaturedMod::getString)
      .collect(Collectors.toList());
  private final List<FeaturedModUpdater> featuredModUpdaters;
  @Inject
  ModService modService;
  @Inject
  PreferencesService preferencesService;
  @Inject
  NotificationService notificationService;
  @Inject
  I18n i18n;
  @Inject
  ApplicationContext applicationContext;
  @Inject
  TaskService taskService;
  @Inject
  FafService fafService;
  @Inject
  FaInitGenerator faInitGenerator;

  public GameUpdaterImpl() {
    featuredModUpdaters = new ArrayList<>();
  }

  @Override
  public GameUpdater addFeaturedModUpdater(FeaturedModUpdater featuredModUpdater) {
    featuredModUpdaters.add(featuredModUpdater);
    return this;
  }

  @Override
  public CompletionStage<Void> update(FeaturedModBean featuredMod, Integer version, Map<String, Integer> featuredModVersions, Set<String> simModUids) {
    // The following ugly code is sponsored by the featured-mod-mess. FAF and Coop are both featured mods - but others,
    // (except fafbeta and fafdevelop) implicitly depend on FAF. So if a non-base mod is being played, make sure FAF is
    // installed.
    CompletableFuture<Void> future;
    List<PatchResult> patchResults = new ArrayList<>();
    if (!NAMES_OF_FEATURED_BASE_MODS.contains(featuredMod.getTechnicalName())) {
      future = modService.getFeaturedMod(FAF.getString())
          .thenCompose(baseMod -> updateFeaturedMod(featuredMod, null))
          .thenAccept(patchResults::add);
    } else {
      future = CompletableFuture.completedFuture(null);
    }

    return future.thenCompose(aVoid -> updateFeaturedMod(featuredMod, version))
        .thenAccept(patchResults::add)
        .thenCompose(aVoid -> updateGameBinaries(patchResults.get(0).getVersion()))
        .thenRun(() -> {
          List<MountPoint> collect = patchResults.stream()
              .flatMap(patchResult -> patchResult.getMountPoints().stream())
              .collect(Collectors.toList());
          faInitGenerator.generateInitFile(collect);
        })
        .thenRun(() -> downloadMissingSimMods(simModUids));
  }

  @Override
  public CompletableFuture<List<FeaturedModBean>> getFeaturedMods() {
    return fafService.getFeaturedMods();
  }

  private CompletionStage<Void> downloadMissingSimMods(Set<String> simModUids) {
    if (simModUids == null || simModUids.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    List<CompletionStage<Void>> simModFutures = simModUids.stream()
        .filter(uid -> !modService.isModInstalled(uid))
        .map(uid -> modService.downloadAndInstallMod(uid))
        .collect(Collectors.toList());
    return CompletableFuture.allOf(simModFutures.toArray(new CompletableFuture[simModFutures.size()]));
  }

  private CompletionStage<PatchResult> updateFeaturedMod(FeaturedModBean featuredMod, Integer version) {
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
