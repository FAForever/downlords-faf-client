package com.faforever.client.mod;

import com.faforever.client.legacy.OnGameTypeInfoListener;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ModService {

  void addOnGameTypeListener(OnGameTypeInfoListener onGameTypeInfoListener);

  List<ModInfoBean> getInstalledMods() throws IOException;

  CompletableFuture<Void> downloadAndInstallMod(String modPath);

  Set<String> getInstalledModUids() throws IOException;
}
