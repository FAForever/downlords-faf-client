package com.faforever.client.patch;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.io.ChecksumMismatchException;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.task.TaskService;
import com.faforever.client.update.Version;
import com.faforever.client.util.ConcurrentUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.ObjectFactory;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static com.faforever.client.game.KnownFeaturedMod.FAF_BETA;
import static com.faforever.client.game.KnownFeaturedMod.FAF_DEVELOP;

@Slf4j
@RequiredArgsConstructor
public class GameUpdaterImpl implements GameUpdater {

  private static final List<String> NAMES_OF_FEATURED_BASE_MODS = Stream.of(FAF, FAF_BETA, FAF_DEVELOP)
      .map(KnownFeaturedMod::getTechnicalName).toList();

  private final List<FeaturedModUpdater> featuredModUpdaters = new ArrayList<>();
  private final ModService modService;
  private final TaskService taskService;
  private final DataPrefs dataPrefs;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final ObjectFactory<GameBinariesUpdateTask> gameBinariesUpdateTaskFactory;

  private ComparableVersion gameVersion;
  private String gameType;

  @Override
  public GameUpdater addFeaturedModUpdater(FeaturedModUpdater featuredModUpdater) {
    featuredModUpdaters.add(featuredModUpdater);
    return this;
  }

  @Override
  public CompletableFuture<Void> update(FeaturedModBean featuredMod, Set<String> simModUIDs,
                                        @Nullable Map<String, Integer> featuredModFileVersions,
                                        @Nullable Integer baseVersion, boolean useReplayFolder) {
    gameType = featuredMod.getTechnicalName();

    // The following ugly code is sponsored by the featured-mod-mess. FAF and Coop are both featured mods - but others,
    // (except fafbeta and fafdevelop) implicitly depend on FAF. So if a non-base mod is being played, make sure FAF is
    // installed.
    CompletableFuture<Void> simModsUpdateFuture = downloadMissingSimMods(simModUIDs);
    CompletableFuture<PatchResult> featuredModUpdateFuture;

    if (!NAMES_OF_FEATURED_BASE_MODS.contains(featuredMod.getTechnicalName())) {
      // Assume that the highest version of a featured mod file is the version of the mod we want.
      // Really don't want to encourage the ability for featuredModFiles to not be packaged together
      Integer featuredModVersion = Optional.ofNullable(featuredModFileVersions).map(Map::values).stream().flatMap(Collection::stream).max(Comparator.nullsLast(Comparator.naturalOrder())).orElse(null);

      featuredModUpdateFuture = simModsUpdateFuture.thenCompose(aVoid -> modService.getFeaturedMod(FAF.getTechnicalName()).toFuture())
          .thenCompose(baseMod -> updateFeaturedMod(baseMod, baseVersion, useReplayFolder))
          .thenCompose(patchResult -> updateGameBinaries(patchResult.getVersion(), useReplayFolder))
          .thenCompose(aVoid -> updateFeaturedMod(featuredMod, featuredModVersion, useReplayFolder));
    } else {
      featuredModUpdateFuture = simModsUpdateFuture.thenCompose(aVoid -> updateFeaturedMod(featuredMod, baseVersion, useReplayFolder))
          .thenCompose(patchResult -> updateGameBinaries(patchResult.getVersion(), useReplayFolder).thenApply(aVoid -> patchResult));
    }

    return featuredModUpdateFuture
        .thenAccept(patchResult -> {
          try {
            createFaPathLuaFile(useReplayFolder);
            copyInitFile(patchResult.getInitFile());
          } catch (IOException e) {
            throw new CompletionException(e);
          }
        }).exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
          boolean allowReplaysWhileInGame = forgedAlliancePrefs.isAllowReplaysWhileInGame();

          if (throwable instanceof UnsupportedOperationException || throwable instanceof ChecksumMismatchException) {
            throw new CompletionException(throwable);
          } else if (allowReplaysWhileInGame) {
            log.warn("Unable to update files and experimental replay feature is turned on " +
                "that allows multiple game instances to run in parallel this is most likely the cause.");
            if (throwable.getCause() instanceof AccessDeniedException) {
              throw new UnsupportedOperationException("Unable to patch Forged Alliance to the required version " +
                  "due to conflicting version running", throwable);
            } else {
              log.warn("Ignored error while updating featured mod due to likely concurrent versions", throwable);
              return null;
            }
          } else {
            throw new CompletionException(throwable);
          }
        });
  }

  private void createFaPathLuaFile(boolean useReplayFolder) throws IOException {
    String installationPath = forgedAlliancePrefs.getInstallationPath().toString().replace("\\", "/");
    String vaultPath = forgedAlliancePrefs.getVaultBaseDirectory().toString().replace("\\", "/");
    String pathFileFormat = """
        fa_path = "%s"
        custom_vault_path = "%s"
        GameType = "%s"
        GameVersion = "%s"
        ClientVersion = "%s"
        """.stripIndent();
    String content = String.format(pathFileFormat, installationPath, vaultPath, gameType, gameVersion.toString(), Version.getCurrentVersion());
    Path baseDirectory;
    if (useReplayFolder) {
      baseDirectory = dataPrefs.getReplaysDirectory();
    } else {
      baseDirectory = dataPrefs.getBaseDataDirectory();
    }
    Files.writeString(baseDirectory.resolve("fa_path.lua"), content);
  }

  private void copyInitFile(Path initFile) throws IOException {
    Files.copy(initFile, initFile.resolveSibling(ForgedAlliancePrefs.INIT_FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
  }

  private CompletableFuture<Void> downloadMissingSimMods(Set<String> simModUids) {
    if (simModUids == null || simModUids.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture.allOf(simModUids.stream()
        .filter(uid -> !modService.isInstalled(uid))
        .map(modService::downloadAndInstallMod).toArray(CompletableFuture[]::new));
  }

  private CompletableFuture<PatchResult> updateFeaturedMod(FeaturedModBean featuredMod, Integer version, boolean useReplayFolder) {
    for (FeaturedModUpdater featuredModUpdater : featuredModUpdaters) {
      return featuredModUpdater.updateMod(featuredMod, version, useReplayFolder);
    }
    throw new UnsupportedOperationException("No updater available for featured mod: " + featuredMod
        + " with version:" + version);
  }

  private CompletableFuture<Void> updateGameBinaries(ComparableVersion version, boolean useReplayFolder) {
    GameBinariesUpdateTask binariesUpdateTask = gameBinariesUpdateTaskFactory.getObject();
    binariesUpdateTask.setVersion(version);
    binariesUpdateTask.setUseReplayFolder(useReplayFolder);
    gameVersion = version;
    return taskService.submitTask(binariesUpdateTask).getFuture();
  }
}
