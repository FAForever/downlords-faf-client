package com.faforever.client.patch;

import com.faforever.client.task.PrioritizedCompletableTask;

import java.nio.file.Path;

public interface GitFeaturedModUpdateTask extends PrioritizedCompletableTask<PatchResult> {
  void setGameRepositoryUrl(String gameRepositoryUri);

  void setRef(String ref);

  void setRepositoryDirectory(Path repositoryDirectory);
}
