package com.faforever.client.patch;

import com.faforever.client.task.CompletableTask;
import org.apache.maven.artifact.versioning.ComparableVersion;

public interface GameBinariesUpdateTask extends CompletableTask<Void> {
  void setVersion(ComparableVersion version);
  void setForReplays(boolean forReplays);
}
