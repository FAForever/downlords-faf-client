package com.faforever.client.ui.preferences.event;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Event to be fired whenever the game directory needs to be set.
 */
public class GameDirectoryChooseEvent {
  private final CompletableFuture<Path> future;

  public GameDirectoryChooseEvent() {
    this(null);
  }

  public GameDirectoryChooseEvent(@Nullable CompletableFuture<Path> future) {
    this.future = future;
  }

  public Optional<CompletableFuture<Path>> getFuture() {
    return Optional.ofNullable(future);
  }
}
