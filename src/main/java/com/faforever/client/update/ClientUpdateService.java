package com.faforever.client.update;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.Nullable;

public interface ClientUpdateService {

  /**
   * Returns information about an available update. Returns {@code null} if no update is available.
   */
  @Nullable
  void checkForUpdateInBackground();

  ComparableVersion getCurrentVersion();
}
