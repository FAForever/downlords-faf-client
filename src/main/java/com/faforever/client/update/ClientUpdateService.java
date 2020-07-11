package com.faforever.client.update;


import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.concurrent.CompletableFuture;

public interface ClientUpdateService {

  /**
   * Returns information about an available newest update. Future contains {@code null} if no update is available.
   */
  CompletableFuture<UpdateInfo> checkForUpdateInBackground();

  ComparableVersion getCurrentVersion();

  ClientUpdateTask updateInBackground(UpdateInfo updateInfo);
}
