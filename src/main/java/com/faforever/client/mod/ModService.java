package com.faforever.client.mod;

import com.faforever.client.patch.MountPoint;
import com.faforever.client.task.CompletableTask;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface ModService {

  void loadInstalledMods();

  ObservableList<Mod> getInstalledMods();

  CompletionStage<Void> downloadAndInstallMod(String uid);

  CompletionStage<Void> downloadAndInstallMod(URL url);

  CompletionStage<Void> downloadAndInstallMod(URL url, DoubleProperty progressProperty, StringProperty titleProperty);

  CompletionStage<Void> downloadAndInstallMod(Mod mod, DoubleProperty progressProperty, StringProperty titleProperty);

  Set<String> getInstalledModUids();

  Set<String> getInstalledUiModsUids();

  void enableSimMods(Set<String> simMods) throws IOException;

  boolean isModInstalled(String uid);

  CompletionStage<Void> uninstallMod(Mod mod);

  Path getPathForMod(Mod mod);

  /**
   * Returns mods available on the server.
   */
  CompletionStage<List<Mod>> getAvailableMods();

  CompletionStage<List<Mod>> getMostDownloadedMods(int count);

  CompletionStage<List<Mod>> getMostLikedMods(int count);

  CompletionStage<List<Mod>> getMostPlayedMods(int count);

  CompletionStage<List<Mod>> getNewestMods(int count);

  CompletionStage<List<Mod>> getMostLikedUiMods(int count);

  CompletionStage<List<Mod>> lookupMod(String string, int maxSuggestions);

  Mod extractModInfo(Path path);

  CompletableTask<Void> uploadMod(Path modPath);

  Image loadThumbnail(Mod mod);

  void evictModsCache();

  ComparableVersion readModVersion(Path modDirectory);

  CompletableFuture<List<FeaturedModBean>> getFeaturedMods();

  CompletableFuture<FeaturedModBean> getFeaturedMod(String gameTypeBeanName);

  List<MountPoint> readMountPoints(InputStream inputStream, Path basePath);
}
