package com.faforever.client.preferences;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ForgedAlliancePrefs {

  private String path;
  private int port;

  public ForgedAlliancePrefs() {
    this.port = 6112;
  }

  public Path getPath() {
    if (path == null) {
      return null;
    }
    return Paths.get(path);
  }

  public int getPort() {
    return port;
  }


  public void setPath(Path path) {
    this.path = path.toAbsolutePath().toString();
  }
}
