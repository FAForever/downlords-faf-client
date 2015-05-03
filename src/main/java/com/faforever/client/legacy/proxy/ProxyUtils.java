package com.faforever.client.legacy.proxy;

public final class ProxyUtils {

  private ProxyUtils() {
    // Utility class
  }

  // TODO document, and think about whether an InetSocketAddress could be translated
  public static int translateToProxyPort(int localPort) {
    return localPort + 1;
  }
}
