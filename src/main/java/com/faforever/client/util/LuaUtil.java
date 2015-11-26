package com.faforever.client.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LuaUtil {

  private static final Pattern QUOTED_TEXT_PATTERN = Pattern.compile("[\"'](.*?)[\"']");

  private LuaUtil() {
    throw new AssertionError("Not instantiatable");
  }

  public static String stripQuotes(String string) {
    if (string == null) {
      return null;
    }

    Matcher matcher = QUOTED_TEXT_PATTERN.matcher(string);
    if (matcher.find()) {
      return matcher.group(1);
    }

    return string;
  }
}
