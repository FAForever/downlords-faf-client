package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.ModInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ModsServerAccessor {

  void connect();

  void disconnect();

  CompletableFuture<List<ModInfo>> searchMod(String name);

}
