package com.faforever.client.mod;

import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ModService {

  void loadInstalledMods();

  ObservableList<ModInfoBean> getInstalledMods() throws IOException;

  CompletableFuture<Void> downloadAndInstallMod(String modPath);

  Set<String> getInstalledModUids() throws IOException;

  Set<String> getInstalledUiModsUids() throws IOException;

  void enableSimMods(Set<String> simMods) throws IOException;
}
