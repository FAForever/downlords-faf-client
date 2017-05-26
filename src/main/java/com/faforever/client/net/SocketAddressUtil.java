package com.faforever.client.net;

import com.google.common.base.Strings;

import java.net.InetSocketAddress;

public final class SocketAddressUtil {

  private static final String TO_STRING_FORMAT = "%s:%s";

  private SocketAddressUtil() {
    throw new AssertionError("Not instantiatable");
  }

  /**
   * Returns &lt;host&gt;:&lt;port&gt;
   */
  public static String toString(InetSocketAddress socketAddress) {
    return String.format(TO_STRING_FORMAT, socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
  }

  public static InetSocketAddress fromString(String string) {
    if (Strings.isNullOrEmpty(string)) {
      return null;
    }
    String[] split = string.split(":");
    return new InetSocketAddress(split[0], Integer.parseInt(split[1]));
  }
}
