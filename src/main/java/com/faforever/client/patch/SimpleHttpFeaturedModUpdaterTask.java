package com.faforever.client.patch;

import com.faforever.client.api.FeaturedModFile;
import com.faforever.client.io.ByteCopier;
import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.mod.ModService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.ResourceLocks;
import com.google.common.hash.Hashing;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.github.nocatch.NoCatch.noCatch;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SimpleHttpFeaturedModUpdaterTask extends CompletableTask<PatchResult> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @Inject
  FafService fafService;
  @Inject
  PreferencesService preferencesService;
  @Inject
  ModService modService;

  private FeaturedModBean featuredMod;
  private Integer version;

  public SimpleHttpFeaturedModUpdaterTask() {
    super(Priority.HIGH);
  }

  @Override
  protected PatchResult call() throws Exception {
    final CompletableFuture<List<MountPoint>> mountPointsFuture = new CompletableFuture<>();
    List<FeaturedModFile> featuredModFiles = fafService.getFeaturedModFiles(featuredMod, version).get();
    featuredModFiles.stream()
        // "bin" is excluded since they contain no file of interest (ini file and executable are generated)
        .filter(featuredModFile -> !"bin".equals(featuredModFile.getGroup()))
        .forEach(featuredModFile -> noCatch(() -> {
          Path targetPath = preferencesService.getFafDataDirectory()
              .resolve(featuredModFile.getGroup())
              .resolve(featuredModFile.getName());

          if (Files.exists(targetPath)
              && featuredModFile.getMd5().equals(com.google.common.io.Files.hash(targetPath.toFile(), Hashing.md5()).toString())) {
            logger.debug("Already up to date: {}", targetPath);
          } else {
            downloadFile(featuredModFile, targetPath);
          }

          try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(targetPath))) {
            for (ZipEntry entry; (entry = zipInputStream.getNextEntry()) != null; ) {
              if (entry.getName().equals("mod_info.lua")) {
                mountPointsFuture.complete(modService.readMountPoints(zipInputStream, targetPath.getParent()));
              }
            }
          }
          mountPointsFuture.completeExceptionally(new IllegalStateException("Mod does not provide a mod_info.lua: " + featuredMod.getTechnicalName()));
        }));

    int maxVersion = featuredModFiles.stream()
        .mapToInt(mod -> Integer.parseInt(mod.getVersion()))
        .max()
        .orElseThrow(() -> new IllegalStateException("No version found"));

    return new PatchResult(new ComparableVersion(String.valueOf(maxVersion)), mountPointsFuture.get());
  }

  private void downloadFile(FeaturedModFile featuredModFile, Path targetPath) throws IOException {
    Files.createDirectories(targetPath.getParent());

    URL url = new URL(featuredModFile.getUrl());
    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
    try (InputStream inputStream = url.openStream();
         OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(targetPath))) {
      ResourceLocks.acquireDownloadLock();
      logger.trace("Downloading {}", url);
      ByteCopier.from(inputStream)
          .to(outputStream)
          .totalBytes(httpURLConnection.getContentLength())
          .copy();
    } finally {
      ResourceLocks.freeDownloadLock();
    }
  }

  private List<MountPoint> readMountPoints(LuaValue modInfo, Path basePath) {
    ArrayList<MountPoint> mountPoints = new ArrayList<>();
    LuaTable mountpoints = modInfo.get("mountpoints").checktable();
    for (LuaValue key : mountpoints.keys()) {
      mountPoints.add(new MountPoint(basePath.resolve(key.tojstring()), mountpoints.get(key).tojstring()));
    }
    return mountPoints;
  }

  public void setFeaturedMod(FeaturedModBean featuredMod) {
    this.featuredMod = featuredMod;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
