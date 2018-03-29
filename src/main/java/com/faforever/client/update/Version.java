package com.faforever.client.update;

public final class Version {
  public static final String VERSION;
  public static final String SNAPSHOT = "snapshot";

  static {
    String version = Version.class.getPackage().getImplementationVersion();
    VERSION = version != null ? version : SNAPSHOT;
  }

  private Version() {

  }
}
