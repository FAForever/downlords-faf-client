package com.faforever.client.mod;

import com.faforever.client.task.CompletableTask;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface ModService {

  void loadInstalledMods();

  ObservableList<ModInfoBean> getInstalledMods();

  void downloadAndInstallMod(String uid);

  CompletionStage<Void> downloadAndInstallMod(URL url);

  CompletionStage<Void> downloadAndInstallMod(URL url, DoubleProperty progressProperty, StringProperty titleProperty);

  CompletionStage<Void> downloadAndInstallMod(ModInfoBean modInfoBean, DoubleProperty progressProperty, StringProperty titleProperty);

  Set<String> getInstalledModUids();

  Set<String> getInstalledUiModsUids();

  void enableSimMods(Set<String> simMods) throws IOException;

  boolean isModInstalled(String uid);

  CompletionStage<Void> uninstallMod(ModInfoBean mod);

  Path getPathForMod(ModInfoBean mod);

  /**
   * Returns mods available on the server.
   */
  CompletionStage<List<ModInfoBean>> getAvailableMods();

  CompletionStage<List<ModInfoBean>> getMostDownloadedMods(int count);

  CompletionStage<List<ModInfoBean>> getMostLikedMods(int count);

  CompletionStage<List<ModInfoBean>> getMostPlayedMods(int count);

  CompletionStage<List<ModInfoBean>> getNewestMods(int count);

  CompletionStage<List<ModInfoBean>> getMostLikedUiMods(int count);

  CompletionStage<List<ModInfoBean>> lookupMod(String string, int maxSuggestions);

  ModInfoBean extractModInfo(Path path);

  CompletableTask<Void> uploadMod(Path modPath);

  Image loadThumbnail(ModInfoBean mod);

  void evictModsCache();

  ComparableVersion readModVersion(Path modDirectory);
}
