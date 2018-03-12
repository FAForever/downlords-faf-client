package com.faforever.client.mod;

import com.faforever.client.task.CompletableTask;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ModService {

  void loadInstalledMods();

  ObservableList<ModVersion> getInstalledModVersions();

  CompletableFuture<Void> downloadAndInstallMod(String uid);

  CompletableFuture<Void> downloadAndInstallMod(URL url);

  CompletableFuture<Void> downloadAndInstallMod(URL url, DoubleProperty progressProperty, StringProperty titleProperty);

  CompletableFuture<Void> downloadAndInstallMod(ModVersion modVersion, DoubleProperty progressProperty, StringProperty titleProperty);

  Set<String> getInstalledModUids();

  Set<String> getInstalledUiModsUids();

  void enableSimMods(Set<String> simMods) throws IOException;

  boolean isModInstalled(String uid);

  CompletableFuture<Void> uninstallMod(ModVersion modVersion);

  Path getPathForMod(ModVersion modVersion);

  CompletableFuture<List<ModVersion>> getHighestRatedUiMods(int count, int page);

  CompletableFuture<List<ModVersion>> getHighestRatedMods(int count, int page);

  CompletableFuture<List<ModVersion>> getNewestMods(int count, int page);

  @NotNull
  ModVersion extractModInfo(Path path);

  @NotNull
  ModVersion extractModInfo(InputStream inputStream, Path basePath);

  CompletableTask<Void> uploadMod(Path modPath);

  Image loadThumbnail(ModVersion modVersion);

  void evictModsCache();

  /**
   * Returns the download size of the specified modVersion in bytes.
   */
  long getModSize(ModVersion modVersion);

  ComparableVersion readModVersion(Path modDirectory);

  CompletableFuture<List<FeaturedMod>> getFeaturedMods();

  CompletableFuture<FeaturedMod> getFeaturedMod(String gameTypeBeanName);

  List<ModVersion> getActivatedSimAndUIMods() throws IOException;

  void overrideActivatedMods(List<ModVersion> modVersions) throws IOException;

  CompletableFuture<List<ModVersion>> findByQuery(SearchConfig searchConfig, int page, int maxSearchResults);

  void evictCache();
}
