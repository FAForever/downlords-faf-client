package com.faforever.client.update;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.regex.Pattern;

import static java.text.MessageFormat.format;

@Slf4j
public final class Version {
  private static String currentVersion;

  private static final String SNAPSHOT_VERSION = "snapshot";
  private static final Pattern SEMVER_PATTERN = Pattern.compile("v?\\d+(\\.\\d+)*[^.]*");
  private static final String UNSPECIFIED_VERSION = "unspecified";

  static {
    String version = Version.class.getPackage().getImplementationVersion();
    currentVersion = version != null ? version : SNAPSHOT_VERSION;

    log.info("The current application version is: {}", currentVersion);
  }

  private Version() {
    // static class
  }

  public static String getCurrentVersion() {
    return currentVersion;
  }

  /**
   * Compares a remote version with the current version of the application.
   *
   * @return true if the remote version is higher than the current version
   */
  public static boolean shouldUpdate(@NonNull String fromVersionRaw, @NonNull String toVersionRaw) {
    log.debug("Comparing current version '{}' to remote version '{}'", currentVersion, toVersionRaw);

    String fromVersion = removePrefix(fromVersionRaw);
    String toVersion = removePrefix(toVersionRaw);

    if (fromVersion.equals(SNAPSHOT_VERSION) || fromVersion.equals(UNSPECIFIED_VERSION)) {
      log.info("Snapshot versions are not to be updated");
      return false;
    }

    if (!SEMVER_PATTERN.matcher(fromVersion).matches()) {
      log.error("fromVersion '{}' is not matching semver pattern", fromVersion);
      // since obviously the app is not properly versioned, throw an exception - this should not happen
      throw new IllegalArgumentException(format("fromVersion ''{0}'' is not matching semver pattern", fromVersion));
    }

    if (!SEMVER_PATTERN.matcher(toVersion).matches()) {
      log.error("toVersion '{}' is not matching semver pattern", toVersion);
      // probably issue on the remote side where we fetched the toVersion - no exception and "just no update"
      return false;
    }

    // Strip the "v" prefix
    ComparableVersion fromComparableVersion = new ComparableVersion(fromVersion);
    ComparableVersion toComparableVersion = new ComparableVersion(toVersion);

    if (toComparableVersion.compareTo(fromComparableVersion) < 1) {
      log.info("fromVersion '{}' is not newer than toVersion '{}'. No update is required.",
          toComparableVersion.getCanonical(), fromComparableVersion.getCanonical());
      return false;
    } else {
      log.info("fromVersion version '{}' is newer than toVersion '{}'. fromVersion should be updated.",
          toComparableVersion.getCanonical(), fromComparableVersion.getCanonical());
      return true;
    }
  }

  private static String removePrefix(String version) {
    if (version.startsWith("v")) {
      return version.substring(1);
    } else {
      return version;
    }
  }
}
