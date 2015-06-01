package com.faforever.client.preferences;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ForgedAlliancePrefs {

  private String path;
  private int port;

  public ForgedAlliancePrefs() {
    this.path = "C:\\Games\\Supreme Commander - Forged Alliance";
    this.port = 6112;
  }

  public Path getPath() {
    return Paths.get(path);
  }

  public int getPort() {
    return port;
  }
}
