package com.faforever.client.map.generator;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface MapGeneratorService {

  public CompletableFuture<String> generateMap();

  @NotNull
  public CompletableFuture<String> generateMap(String mapName);

  @NotNull
  public CompletableFuture<String> generateMap(long seed, String version);

  @NotNull
  public boolean isGeneratedMap(String mapName);

}
