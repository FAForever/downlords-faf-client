package com.faforever.client.mod;

import com.faforever.client.task.CompletableTask;
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

  ObservableList<Mod> getInstalledMods();

  CompletableFuture<Void> downloadAndInstallMod(String uid);

  CompletableFuture<Void> downloadAndInstallMod(URL url);

  CompletableFuture<Void> downloadAndInstallMod(URL url, DoubleProperty progressProperty, StringProperty titleProperty);

  CompletableFuture<Void> downloadAndInstallMod(Mod mod, DoubleProperty progressProperty, StringProperty titleProperty);

  Set<String> getInstalledModUids();

  Set<String> getInstalledUiModsUids();

  void enableSimMods(Set<String> simMods) throws IOException;

  boolean isModInstalled(String uid);

  CompletableFuture<Void> uninstallMod(Mod mod);

  Path getPathForMod(Mod mod);

  /**
   * Returns mods available on the server.
   */
  CompletableFuture<List<Mod>> getAvailableMods();

  CompletableFuture<List<Mod>> getMostDownloadedMods(int count);

  CompletableFuture<List<Mod>> getMostLikedMods(int count);

  CompletableFuture<List<Mod>> getMostPlayedMods(int count);

  CompletableFuture<List<Mod>> getNewestMods(int count);

  CompletableFuture<List<Mod>> getMostLikedUiMods(int count);

  CompletableFuture<List<Mod>> lookupMod(String string, int maxSuggestions);

  @NotNull
  Mod extractModInfo(Path path);

  @NotNull
  Mod extractModInfo(InputStream inputStream, Path basePath);

  CompletableTask<Void> uploadMod(Path modPath);

  Image loadThumbnail(Mod mod);

  void evictModsCache();

  ComparableVersion readModVersion(Path modDirectory);

  CompletableFuture<List<FeaturedModBean>> getFeaturedMods();

  CompletableFuture<FeaturedModBean> getFeaturedMod(String gameTypeBeanName);
}
