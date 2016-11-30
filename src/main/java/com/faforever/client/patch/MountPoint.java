package com.faforever.client.patch;

import java.nio.file.Path;

public class MountPoint {
  private final Path directory;
  private final String mountPath;

  public MountPoint(Path directory, String mountPath) {
    this.directory = directory;
    this.mountPath = mountPath;
  }

  /**
   * Returns the directory on the local file system that should be mounted into the virtual file system of the game.
   */
  public Path getDirectory() {
    return directory;
  }

  /**
   * Returns the absolute mount path (starting with {@code /}) to which the directory returned by
   * {@link #getDirectory()} should be mounted to.
   */
  public String getMountPath() {
    return mountPath;
  }
}
