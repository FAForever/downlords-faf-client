package com.faforever.client.update;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.Nullable;

public class MockClientUpdateService implements ClientUpdateService {

  @Nullable
  @Override
  public void checkForUpdateInBackground() {

  }

  @Override
  public ComparableVersion getCurrentVersion() {
    return new ComparableVersion("dev");
  }
}
