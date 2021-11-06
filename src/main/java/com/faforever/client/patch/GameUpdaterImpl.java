package com.faforever.client.patch;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.exception.ProgrammingError;
import com.faforever.client.game.FaInitGenerator;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.commons.mod.MountInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.faforever.client.game.KnownFeaturedMod.BALANCE_TESTING;
import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static com.faforever.client.game.KnownFeaturedMod.FAF_BETA;
import static com.faforever.client.game.KnownFeaturedMod.FAF_DEVELOP;
import static com.faforever.client.game.KnownFeaturedMod.LADDER_1V1;

@Slf4j
@RequiredArgsConstructor
public class GameUpdaterImpl implements GameUpdater {

  private static final List<String> NAMES_OF_FEATURED_BASE_MODS = Stream.of(FAF, FAF_BETA, FAF_DEVELOP, BALANCE_TESTING, LADDER_1V1)
      .map(KnownFeaturedMod::getTechnicalName)
      .collect(Collectors.toList());

  private final List<FeaturedModUpdater> featuredModUpdaters = new ArrayList<>();
  private final ModService modService;
  private final ApplicationContext applicationContext;
  private final TaskService taskService;
  private final FaInitGenerator faInitGenerator;
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;

  @Override
  public GameUpdater addFeaturedModUpdater(FeaturedModUpdater featuredModUpdater) {
    featuredModUpdaters.add(featuredModUpdater);
    return this;
  }

  @Override
  public CompletableFuture<Void> update(FeaturedModBean featuredMod, Integer version, Map<String, Integer> featuredModVersions, Set<String> simModUids) {
    // The following ugly code is sponsored by the featured-mod-mess. FAF and Coop are both featured mods - but others,
    // (except fafbeta and fafdevelop) implicitly depend on FAF. So if a non-base mod is being played, make sure FAF is
    // installed.
    List<PatchResult> patchResults = new ArrayList<>();

    CompletableFuture<Void> future = updateFeaturedMod(featuredMod, version)
        .thenAccept(patchResults::add)
        .thenCompose(s -> downloadMissingSimMods(simModUids));

    if (!NAMES_OF_FEATURED_BASE_MODS.contains(featuredMod.getTechnicalName())) {
      future = future.thenCompose(aVoid -> modService.getFeaturedMod(FAF.getTechnicalName()))
          .thenCompose(baseMod -> updateFeaturedMod(baseMod, version))
          .thenAccept(patchResults::add);
    }

    verifyUniformModFormat(patchResults);

    return future
        .thenCompose(s -> updateGameBinaries(patchResults.get(patchResults.size() - 1).getVersion()))
        .thenRun(() -> {
          try {
            if (patchResults.stream().noneMatch(patchResult -> patchResult.getLegacyInitFile() != null)) {
              generateInitFile(patchResults);
            } else {
              Path initFile = patchResults.stream()
                  .map(PatchResult::getLegacyInitFile)
                  .filter(Objects::nonNull)
                  .findFirst()
                  .orElseThrow(() -> new ProgrammingError("No legacy init file is available"));


              createFaPathLuaFile(initFile.getParent().getParent());
              copyLegacyInitFile(initFile);
            }
          } catch (IOException e) {
            throw new CompletionException(e);
          }
        }).exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
          boolean allowReplaysWhileInGame = preferencesService.getPreferences().getForgedAlliance().isAllowReplaysWhileInGame();
          if (throwable.getCause() instanceof AccessDeniedException && allowReplaysWhileInGame) {
            log.info("Unable to update files and experimental replay feature is turned on " +
                "that allows multiple game instances to run in parallel this is most likely the cause.");
            throw new UnsupportedOperationException("Unable to patch Forged Alliance to the required version " +
                "due to conflicting version running", throwable);
          } else if (!allowReplaysWhileInGame) {
            log.warn("Game files not accessible", throwable);
            notificationService.addImmediateErrorNotification(throwable, "error.game.filesNotAccessible");
          } else {
            log.info("Game files not accessible most likely due to concurrent game instances", throwable);
          }
          return null;
        });
  }

  private void createFaPathLuaFile(Path parent) throws IOException {
    ForgedAlliancePrefs forgedAlliancePrefs = preferencesService.getPreferences().getForgedAlliance();
    String installationPath = forgedAlliancePrefs.getInstallationPath().toString().replace("\\", "/");
    String vaultPath = forgedAlliancePrefs.getVaultBaseDirectory().toString().replace("\\", "/");
    String pathFileFormat = """
        fa_path = "%s"
        custom_vault_path = "%s"
        """.stripIndent();
    String content = String.format(pathFileFormat, installationPath, vaultPath);
    Files.writeString(parent.resolve("fa_path.lua"), content);
  }

  private void generateInitFile(List<PatchResult> patchResults) throws IOException {
    List<MountInfo> mountPoints = patchResults.stream()
        .flatMap(patchResult -> Optional.ofNullable(patchResult.getMountInfos()).orElseThrow(() -> new ProgrammingError("No mount infos where available")).stream())
        .collect(Collectors.toList());

    Set<String> hookDirectories = patchResults.stream()
        .flatMap(patchResult -> Optional.ofNullable(patchResult.getHookDirectories()).orElseThrow(() -> new ProgrammingError("No mount infos where available")).stream())
        .collect(Collectors.toSet());

    faInitGenerator.generateInitFile(mountPoints, hookDirectories);
  }

  private void copyLegacyInitFile(Path initFile) throws IOException {
    Files.copy(initFile, initFile.resolveSibling(ForgedAlliancePrefs.INIT_FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
  }

  /**
   * Older versions of featured mods didn't provide a mod info file but provided an init file instead. New featured mod
   * versions provide an mod info file from which an init file is generated. Mixing these two kind of mods doesn't work,
   * which is what this method ensures.
   */
  private void verifyUniformModFormat(List<PatchResult> patchResults) {
    long modsWithLegacyInitFile = patchResults.stream().filter(patchResult -> patchResult.getLegacyInitFile() != null).count();
    if (modsWithLegacyInitFile != 0 && modsWithLegacyInitFile != patchResults.size()) {
      throw new IllegalStateException("Legacy and non-legacy mods can't be mixed.");
    }
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

  private CompletableFuture<PatchResult> updateFeaturedMod(FeaturedModBean featuredMod, Integer version) {
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
