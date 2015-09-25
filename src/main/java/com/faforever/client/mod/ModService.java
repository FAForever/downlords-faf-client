package com.faforever.client.mod;

import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ModService {

  ObservableSet<ModInfoBean> getAvailableMods();

  void loadInstalledMods();

  ObservableList<ModInfoBean> getInstalledMods() throws IOException;

  CompletableFuture<Void> downloadAndInstallMod(String modPath);

  Set<String> getInstalledModUids() throws IOException;

  Set<String> getInstalledUiModsUids() throws IOException;

  void enableSimMods(Set<String> simMods) throws IOException;

  /**
   * Requests the server to send mod information. Since the server sends them one by one and it's unknown how many, this
   * method does not return a future. Instead, callers should listen on the set returned by {@link
   * #getAvailableMods()}.
   */
  void requestMods();
}
