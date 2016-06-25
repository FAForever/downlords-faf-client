package com.faforever.client.update;

import org.apache.maven.artifact.versioning.ComparableVersion;

public class MockClientUpdateService implements ClientUpdateService {

  @Override
  public void checkForUpdateInBackground() {

  }

  @Override
  public ComparableVersion getCurrentVersion() {
    return new ComparableVersion("dev");
  }
}
