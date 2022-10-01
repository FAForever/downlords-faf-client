package com.faforever.client.util;

import java.util.List;

public class JavaUtil {
  public static final String CLASSPATH_SEPARATOR = System.getProperty("path.separator");
  public static final List<String> CLASS_PATH_LIST = getClasspath();

  private static List<String> getClasspath() {
    String classPath = System.getProperty("java.class.path");
    return List.of(classPath.split(CLASSPATH_SEPARATOR));
  }
}
