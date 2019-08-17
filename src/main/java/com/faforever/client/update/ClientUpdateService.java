package com.faforever.client.update;


import java.util.concurrent.CompletableFuture;

public interface ClientUpdateService {

  /**
   * Returns information about an available mandatory update. Returns {@code null} if no update is available.
   */
  CompletableFuture<UpdateInfo> checkForMandatoryUpdate();

  /**
   * Checks for regular update and creates an UpdateNotification
   */
  void checkForRegularUpdateInBackground();

  String getCurrentVersion();

  DownloadUpdateTask downloadAndInstallInBackground(UpdateInfo updateInfo);
}
