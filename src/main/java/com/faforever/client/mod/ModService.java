package com.faforever.client.mod;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ModService {

  void loadInstalledMods();

  ObservableList<ModInfoBean> getInstalledMods();

  CompletableFuture<Void> downloadAndInstallMod(URL url);

  CompletableFuture<Void> downloadAndInstallMod(URL url, DoubleProperty progressProperty, StringProperty titleProperty);

  CompletableFuture<Void> downloadAndInstallMod(ModInfoBean modInfoBean, DoubleProperty progressProperty, StringProperty titleProperty);

  Set<String> getInstalledModUids();

  Set<String> getInstalledUiModsUids();

  void enableSimMods(Set<String> simMods) throws IOException;

  CompletableFuture<List<ModInfoBean>> requestMods();

  boolean isModInstalled(String uid);

  CompletableFuture<Void> uninstallMod(ModInfoBean mod);

  Path getPathForMod(ModInfoBean mod);

  CompletableFuture<List<ModInfoBean>> searchMod(String name);
}
