package com.faforever.client.util;

import java.net.InetSocketAddress;

public final class SocketAddressUtil {

  private static final String TO_STRING_FORMAT = "%s:%s";

  private SocketAddressUtil() {
    // Utility class
  }

  public static String toString(InetSocketAddress socketAddress) {
    return String.format(TO_STRING_FORMAT, socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
  }
}
