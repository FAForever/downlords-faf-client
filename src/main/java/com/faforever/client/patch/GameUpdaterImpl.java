package com.faforever.client.patch;

import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.io.ChecksumMismatchException;
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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import static com.faforever.client.game.KnownFeaturedMod.FAF;
import static com.faforever.client.game.KnownFeaturedMod.FAF_BETA;
import static com.faforever.client.game.KnownFeaturedMod.FAF_DEVELOP;

@Slf4j
@RequiredArgsConstructor
@Component
@Profile("!local")
public class GameUpdaterImpl implements GameUpdater {

  private static final List<String> NAMES_OF_FEATURED_BASE_MODS = Stream.of(FAF, FAF_BETA, FAF_DEVELOP)
      .map(KnownFeaturedMod::getTechnicalName).toList();

  private final TaskService taskService;
  private final DataPrefs dataPrefs;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final ObjectFactory<GameBinariesUpdateTask> gameBinariesUpdateTaskFactory;
  private final FeaturedModUpdater featuredModUpdater;

  @Override
  public CompletableFuture<Void> update(String featuredModName,
                                        @Nullable Map<String, Integer> featuredModFileVersions,
                                        @Nullable Integer baseVersion, boolean forReplays) {
    // The following ugly code is sponsored by the featured-mod-mess. FAF and Coop are both featured mods - but others,
    // (except fafbeta and fafdevelop) implicitly depend on FAF. So if a non-base mod is being played, make sure FAF is
    // installed.
    CompletableFuture<PatchResult> featuredModUpdateFuture;

    if (!NAMES_OF_FEATURED_BASE_MODS.contains(featuredModName)) {
      // Assume that the highest version of a featured mod file is the version of the mod we want.
      // Really don't want to encourage the ability for featuredModFiles to not be packaged together
      Integer featuredModVersion = Optional.ofNullable(featuredModFileVersions).map(Map::values).stream().flatMap(Collection::stream).max(Comparator.nullsLast(Comparator.naturalOrder())).orElse(null);

      featuredModUpdateFuture = updateFeaturedMod(FAF.getTechnicalName(), baseVersion, forReplays)
                                                   .thenCompose(patchResult -> updateGameBinaries(patchResult.version(),
                                                                                                  forReplays))
                                                   .thenCompose(
                                                       aVoid -> updateFeaturedMod(featuredModName, featuredModVersion,
                                                                                  forReplays));
    } else {
      featuredModUpdateFuture = updateFeaturedMod(featuredModName, baseVersion, forReplays)
                                                   .thenCompose(patchResult -> updateGameBinaries(patchResult.version(),
                                                                                                  forReplays).thenApply(
                                                       aVoid -> patchResult));
    }

    return featuredModUpdateFuture
        .thenAccept(patchResult -> {
          try {
            createFaPathLuaFile(featuredModName, patchResult.version(), forReplays);
            copyInitFile(patchResult.initFile());
          } catch (IOException e) {
            throw new CompletionException(e);
          }
        }).exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);

          if (throwable instanceof UnsupportedOperationException || throwable instanceof ChecksumMismatchException) {
            throw new CompletionException(throwable);
          } else if (throwable.getCause() instanceof AccessDeniedException) {
              throw new UnsupportedOperationException("Unable to patch Forged Alliance to the required version " +
                  "due to conflicting version running", throwable);
          } else {
            throw new CompletionException(throwable);
          }
        });
  }

  private void createFaPathLuaFile(String featuredModName, ComparableVersion gameVersion,
                                   boolean forReplays) throws IOException {
    String installationPathString = forgedAlliancePrefs.getInstallationPath().toString().replace("\\", "/");
    String vaultPathString = forgedAlliancePrefs.getVaultBaseDirectory().toString().replace("\\", "/");
    if (forgedAlliancePrefs.isRelativeGamePaths()) {
      String homeDirString = Path.of(System.getProperty("user.home")).toString().replace("\\", "/");
      if (installationPathString.startsWith(homeDirString)) {
        installationPathString = installationPathString.substring(homeDirString.length());
      }
      if (vaultPathString.startsWith(homeDirString)) {
        vaultPathString = vaultPathString.substring(homeDirString.length());
      }
    }

    String pathFileFormat = """
        fa_path = "%s"
        custom_vault_path = "%s"
        GameType = "%s"
        GameVersion = "%s"
        ClientVersion = "%s"
        """.stripIndent();
    String content = String.format(pathFileFormat, installationPathString, vaultPathString, featuredModName, gameVersion.toString(),
                                   Version.getCurrentVersion());
    Path baseDirectory;
    if (forReplays) {
      baseDirectory = dataPrefs.getReplayDataDirectory();
    } else {
      baseDirectory = dataPrefs.getBaseDataDirectory();
    }
    Files.writeString(baseDirectory.resolve("fa_path.lua"), content);
  }

  private void copyInitFile(Path initFile) throws IOException {
    Files.copy(initFile, initFile.resolveSibling(ForgedAlliancePrefs.INIT_FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
  }

  private CompletableFuture<PatchResult> updateFeaturedMod(String featuredModName, Integer version,
                                                           boolean forReplays) {
    if (featuredModUpdater == null) {
      throw new UnsupportedOperationException("No updater available for featured mods");
    }
    return featuredModUpdater.updateMod(featuredModName, version, forReplays);
  }

  private CompletableFuture<Void> updateGameBinaries(ComparableVersion version, boolean forReplays) {
    GameBinariesUpdateTask binariesUpdateTask = gameBinariesUpdateTaskFactory.getObject();
    binariesUpdateTask.setVersion(version);
    binariesUpdateTask.setForReplays(forReplays);
    return taskService.submitTask(binariesUpdateTask).getFuture();
  }
}
