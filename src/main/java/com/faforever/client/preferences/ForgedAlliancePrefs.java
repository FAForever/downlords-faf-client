package com.faforever.client.preferences;

public class ForgedAlliancePrefs {

  private String path;
  private int port;

  public ForgedAlliancePrefs() {
    this.path = "C:\\Games\\Supreme Commander - Forged Alliance";
    this.port = 6112;
  }

  public String getPath() {
    return path;
  }

  public int getPort() {
    return port;
  }
}
