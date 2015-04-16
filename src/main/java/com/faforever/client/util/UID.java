package com.faforever.client.util;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;

public class UID {

  static {
    try {
      // FIXME bundle and reference properly
      System.loadLibrary("uid");
    } catch (UnsatisfiedLinkError e) {
      throw new RuntimeException(e);
    }
  }

  public interface UidLibrary extends StdCallLibrary {

    UidLibrary INSTANCE = (UidLibrary) Native.loadLibrary("uid", UidLibrary.class);

    String uid(String salt);
  }

  private UID() {
    // Utility class
  }

  public static String generate(String salt) {
    return UidLibrary.INSTANCE.uid(salt);
  }

}
