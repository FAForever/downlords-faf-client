package com.faforever.client.update;

import com.faforever.client.FafClientApplication;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;


@Lazy
@Service
@Profile(FafClientApplication.POFILE_OFFLINE)
public class MockClientUpdateService implements ClientUpdateService {

  @Override
  public void checkForUpdateInBackground() {

  }

  @Override
  public ComparableVersion getCurrentVersion() {
    return new ComparableVersion("dev");
  }
}
