package com.faforever.client.util;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;

import java.nio.file.Path;

public class UID {

  static {
      // FIXME bundle and reference properly
      System.loadLibrary("uid");
    //throwing RuntimeException here results in ExceptionInInitializerError - which has no message
  }

  public interface UidLibrary extends StdCallLibrary {

    UidLibrary INSTANCE = (UidLibrary) Native.loadLibrary("uid", UidLibrary.class);

    String uid(String salt, String logFile);
  }

  private UID() {
    // Utility class
  }

  public static String generate(String salt, Path logFile) {
    return UidLibrary.INSTANCE.uid(salt, logFile.toAbsolutePath().toString());
  }

}
