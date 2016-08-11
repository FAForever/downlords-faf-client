package com.faforever.client.mod;

import com.faforever.client.task.CompletableTask;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public interface ModService {

  void loadInstalledMods();

  ObservableList<ModInfoBean> getInstalledMods();

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

  CompletionStage<List<ModInfoBean>> getNewestMods(int count);

  CompletionStage<List<ModInfoBean>> getMostLikedUiMods(int count);

  CompletionStage<List<ModInfoBean>> lookupMod(String string, int maxSuggestions);

  ModInfoBean extractModInfo(Path path) throws IOException;

  CompletableTask<Void> uploadMod(Path modPath, Consumer<Float> progressListener);

  Image loadThumbnail(ModInfoBean mod);
}
