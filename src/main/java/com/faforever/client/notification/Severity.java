package com.faforever.client.notification;

public enum Severity {
  // Severities must be kept in order

  /**
   * Use this severity to inform the user about positive events he's probably interested in and that doesn't occur often
   * (like an available software update) or only on request (like a successful installation of a map or a mod).
   * <p>
   * Do NOT use it for boring events or ones that occur often, like "Connected to FAF server".
   */
  INFO,

  /**
   * Use this severity to inform the user about recoverable errors (something that went wrong but can be fixed) that
   * requires his action to fix.
   */
  WARN,

  /**
   * Use this severity to inform the user about unrecoverable errors. That is, something that went wrong and cannot be
   * fixed.
   */
  ERROR
}
