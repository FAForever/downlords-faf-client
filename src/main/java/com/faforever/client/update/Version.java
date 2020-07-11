package com.faforever.client.update;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;

@Slf4j
public final class Version {
  @VisibleForTesting
  public static ComparableVersion currentVersion;

  @VisibleForTesting
  static final ComparableVersion UNSPECIFIED_VERSION = new ComparableVersion("0.0.0");

  static {
    String version = Version.class.getPackage().getImplementationVersion();
    currentVersion = version != null ? new ComparableVersion(version) : UNSPECIFIED_VERSION;

    log.info("The current application version is: {}", currentVersion);
  }

  private Version() {
    // static class
  }

  public static ComparableVersion getCurrentVersion() {
    return currentVersion;
  }

  /**
   * Compares a remote version with the current version of the application.
   *
   * @return true if the remote version is higher than the current version
   */
  public static boolean shouldUpdate(ComparableVersion fromVersion, ComparableVersion toVersion) {
    log.debug("Comparing version '{}' to remote version '{}'", fromVersion, toVersion);

    if (fromVersion.equals(UNSPECIFIED_VERSION)) {
      log.info("Snapshot versions are not to be updated");
      return false;
    }

    if (toVersion.compareTo(fromVersion) < 1) {
      log.info("fromVersion '{}' is not newer than toVersion '{}'. No update is required.",
          toVersion.getCanonical(), fromVersion.getCanonical());
      return false;
    }
    log.info("fromVersion version '{}' is newer than toVersion '{}'. Client should be updated.",
        toVersion.getCanonical(), fromVersion.getCanonical());
    return true;
  }
}
