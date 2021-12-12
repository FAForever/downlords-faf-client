package com.faforever.client.patch;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.ConcurrentUtil;
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
  private final PreferencesService preferencesService;
  private final NotificationService notificationService;

  @Override
  public GameUpdater addFeaturedModUpdater(FeaturedModUpdater featuredModUpdater) {
    featuredModUpdaters.add(featuredModUpdater);
    return this;
  }

  @Override
  public CompletableFuture<Void> update(FeaturedModBean featuredMod, Integer version, Set<String> simModUIDs) {
    // The following ugly code is sponsored by the featured-mod-mess. FAF and Coop are both featured mods - but others,
    // (except fafbeta and fafdevelop) implicitly depend on FAF. So if a non-base mod is being played, make sure FAF is
    // installed.
    CompletableFuture<Void> simModsUpdateFuture = downloadMissingSimMods(simModUIDs);
    CompletableFuture<PatchResult> featuredModUpdateFuture;

    if (!NAMES_OF_FEATURED_BASE_MODS.contains(featuredMod.getTechnicalName())) {
      featuredModUpdateFuture = simModsUpdateFuture.thenCompose(aVoid -> modService.getFeaturedMod(FAF.getTechnicalName()))
          .thenCompose(baseMod -> updateFeaturedMod(baseMod, null))
          .thenCompose(patchResult -> updateGameBinaries(patchResult.getVersion()))
          .thenCompose(aVoid -> updateFeaturedMod(featuredMod, version));
    } else {
      featuredModUpdateFuture = simModsUpdateFuture.thenCompose(aVoid -> updateFeaturedMod(featuredMod, version))
          .thenCompose(patchResult -> updateGameBinaries(patchResult.getVersion()).thenApply(aVoid -> patchResult));
    }

    return featuredModUpdateFuture
        .thenAccept(patchResult -> {
          try {
            createFaPathLuaFile();
            copyInitFile(patchResult.getInitFile());
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
          } else if (throwable instanceof UnsupportedOperationException) {
            throw new CompletionException(throwable);
          } else if (!allowReplaysWhileInGame) {
            log.warn("Game files not accessible", throwable);
            notificationService.addImmediateErrorNotification(throwable, "error.game.filesNotAccessible");
          } else {
            log.info("Game files not accessible most likely due to concurrent game instances", throwable);
          }
          return null;
        });
  }

  private void createFaPathLuaFile() throws IOException {
    ForgedAlliancePrefs forgedAlliancePrefs = preferencesService.getPreferences().getForgedAlliance();
    String installationPath = forgedAlliancePrefs.getInstallationPath().toString().replace("\\", "/");
    String vaultPath = forgedAlliancePrefs.getVaultBaseDirectory().toString().replace("\\", "/");
    String pathFileFormat = """
        fa_path = "%s"
        custom_vault_path = "%s"
        """.stripIndent();
    String content = String.format(pathFileFormat, installationPath, vaultPath);
    Files.writeString(preferencesService.getFafDataDirectory().resolve("fa_path.lua"), content);
  }

  private void copyInitFile(Path initFile) throws IOException {
    Files.copy(initFile, initFile.resolveSibling(ForgedAlliancePrefs.INIT_FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
  }

  private CompletableFuture<Void> downloadMissingSimMods(Set<String> simModUids) {
    if (simModUids == null || simModUids.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture.allOf(simModUids.stream()
        .filter(uid -> !modService.isModInstalled(uid))
        .map(modService::downloadAndInstallMod).toArray(CompletableFuture[]::new));
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
