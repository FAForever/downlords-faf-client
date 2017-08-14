package com.faforever.client.update;

public final class Version {
  public static final String VERSION;

  static {
    String version = Version.class.getPackage().getImplementationVersion();
    VERSION = version != null ? version : "snapshot";
  }

  private Version() {

  }
}
