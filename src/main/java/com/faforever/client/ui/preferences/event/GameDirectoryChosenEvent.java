package com.faforever.client.ui.preferences.event;

import javax.annotation.Nullable;
import java.nio.file.Path;

public class GameDirectoryChosenEvent {
  @Nullable
  private Path path;

  public GameDirectoryChosenEvent(@Nullable Path path) {
    this.path = path;
  }

  @Nullable
  public Path getPath() {
    return path;
  }
}
