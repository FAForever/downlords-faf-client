package com.faforever.client.patch;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface GameUpdateService {

  /**
   * @param modVersions a map of indices ("1","2","3","4"...) to version numbers. Don't ask me what these indices map
   * to.
   * @param simModUids a list of sim mod UIDs to update
   */
  CompletableFuture<Void> updateInBackground(String gameType, Integer version, Map<String, Integer> modVersions, Set<String> simModUids);

  CompletableFuture<Void> checkForUpdateInBackground();


}
