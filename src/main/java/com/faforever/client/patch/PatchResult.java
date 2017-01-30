package com.faforever.client.patch;


import com.faforever.commons.mod.MountPoint;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.List;

public class PatchResult {
  private final ComparableVersion version;
  private final List<MountPoint> mountPoints;
  private final List<String> hookDirectories;

  public PatchResult(ComparableVersion version, List<MountPoint> mountPoints, List<String> hookDirectories) {
    this.version = version;
    this.mountPoints = mountPoints;
    this.hookDirectories = hookDirectories;
  }

  public ComparableVersion getVersion() {
    return version;
  }

  public List<MountPoint> getMountPoints() {
    return mountPoints;
  }

  public List<String> getHookDirectories() {
    return hookDirectories;
  }
}
