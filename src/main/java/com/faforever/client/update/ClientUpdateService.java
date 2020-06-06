package com.faforever.client.update;


import java.util.concurrent.CompletableFuture;

public interface ClientUpdateService {

  /**
   * Returns information about an available newest update. Returns {@code null} if no update is available.
   */
  CompletableFuture<UpdateInfo> getNewestUpdate();

  void checkForUpdateInBackground();

  String getCurrentVersion();

  ClientUpdateTask updateInBackground(UpdateInfo updateInfo);
}
