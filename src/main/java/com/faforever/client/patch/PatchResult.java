package com.faforever.client.patch;

import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.List;

public class PatchResult {
  private final ComparableVersion version;
  private final List<MountPoint> mountPoints;

  public PatchResult(ComparableVersion version, List<MountPoint> mountPoints) {
    this.version = version;
    this.mountPoints = mountPoints;
  }

  public ComparableVersion getVersion() {
    return version;
  }

  public List<MountPoint> getMountPoints() {
    return mountPoints;
  }
}
