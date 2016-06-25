package com.faforever.client.update;

import org.apache.maven.artifact.versioning.ComparableVersion;

public interface ClientUpdateService {

  /**
   * Returns information about an available update. Returns {@code null} if no update is available.
   */
  void checkForUpdateInBackground();

  ComparableVersion getCurrentVersion();
}
