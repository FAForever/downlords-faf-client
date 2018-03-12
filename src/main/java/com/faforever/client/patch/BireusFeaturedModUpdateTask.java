package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.io.DownloadService;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModService;
import com.faforever.client.mod.ModVersion;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.brutus5000.bireus.BireusClient;
import net.brutus5000.bireus.service.PatchEventListener;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class BireusFeaturedModUpdateTask extends CompletableTask<PatchResult> {

  private static final String NON_WORD_CHARACTER_PATTERN = "[^\\w]";
  private static final String MOD_INFO_LUA = "mod_info.lua";
  private final PatchEventListener patchEventListener;
  private final BireusDownloadServiceAdapter bireusDownloadService;
  private final ModService modService;
  @Nullable
  private Integer version;
  private FeaturedMod featuredMod;
  private PreferencesService preferencesService;
  private String checkedOutVersion;

  public BireusFeaturedModUpdateTask(PreferencesService preferencesService, DownloadService downloadService, I18n i18n, ModService modService) {
    super(Priority.HIGH);
    this.preferencesService = preferencesService;
    bireusDownloadService = new BireusDownloadServiceAdapter(downloadService, this::updateProgress);

    patchEventListener = new PatchEventListener() {

      @Override
      public void error(String message) {
        // TODO display message to user
      }

      @Override
      public void beginCheckoutVersion(String version) {
        updateTitle(i18n.get("updater.taskTitle"));
      }

      @Override
      public void beginDownloadPatch(URL url) {
        updateMessage(i18n.get("updater.downloadingFile", url));
      }

      @Override
      public void beginPatchingFile(Path path) {
        updateMessage(i18n.get("updater.patchingFile", path));
      }

      @Override
      public void crcMismatch(Path patchPath) {
        // TODO display message to user
      }

      @Override
      public void checkedOutAlready(String version) {
        BireusFeaturedModUpdateTask.this.checkedOutVersion = version;
      }

      @Override
      public void finishCheckoutVersion(String version) {
        BireusFeaturedModUpdateTask.this.checkedOutVersion = version;
      }
    };
    this.modService = modService;
  }


  @Override
  protected PatchResult call() throws Exception {
    String repoDirName = featuredMod.getBireusUrl().toString().replaceAll(NON_WORD_CHARACTER_PATTERN, "");
    Path repositoryPath = preferencesService.getPatchReposDirectory().resolve(repoDirName);

    ResourceLocks.acquireDiskLock();
    try {
      BireusClient bireusClient = initBireus(repositoryPath);
      if (version == null) {
        bireusClient.checkoutLatestVersion();
      } else {
        bireusClient.checkoutVersion(String.valueOf(version));
      }
    } finally {
      ResourceLocks.freeDiskLock();
    }

    Path modInfoLuaFile = repositoryPath.resolve(MOD_INFO_LUA);

    // Older versions do not have a mod info file. Their init file can't be generated, so we require the mod to provide its own
    if (!Files.exists(modInfoLuaFile)) {
      Path initFile = repositoryPath.resolve("bin/init_" + featuredMod.getTechnicalName() + ".lua");
      Assert.isTrue(Files.exists(initFile), "Neither '" + MOD_INFO_LUA + "' nor '" + initFile.getFileName() + "' could be found.");

      return PatchResult.withLegacyInitFile(new ComparableVersion(checkedOutVersion), initFile);
    }

    try (InputStream inputStream = Files.newInputStream(modInfoLuaFile)) {
      ModVersion modVersion = modService.extractModInfo(inputStream, repositoryPath);
      return PatchResult.fromModInfo(modService.readModVersion(repositoryPath), modVersion.getMountInfos(), modVersion.getHookDirectories());
    }
  }

  @SneakyThrows
  private BireusClient initBireus(Path repositoryPath) {
    if (Files.notExists(repositoryPath)) {
      Files.createDirectories(repositoryPath);
      return BireusClient.getFromURL(featuredMod.getBireusUrl(), repositoryPath, patchEventListener, bireusDownloadService);
    }
    return new BireusClient(repositoryPath, patchEventListener, bireusDownloadService);
  }

  public void setVersion(@Nullable Integer version) {
    this.version = version;
  }

  public void setFeaturedMod(FeaturedMod featuredMod) {
    this.featuredMod = featuredMod;
  }
}
