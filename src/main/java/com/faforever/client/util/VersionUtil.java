package com.faforever.client.util;

import org.apache.commons.lang3.StringUtils;

public final class VersionUtil {

  private VersionUtil() {
    throw new AssertionError("Not instantiatable");
  }

  public static String getVersion(Class<?> aClass) {
    return String.format("dfaf-%s", StringUtils.defaultString(aClass.getPackage().getImplementationVersion(), "dev"));
  }
}
