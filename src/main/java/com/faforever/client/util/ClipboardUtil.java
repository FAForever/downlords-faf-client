package com.faforever.client.util;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public final class ClipboardUtil {

  public static void copyToClipboard(String toCopy) {
    final Clipboard clipboard = Clipboard.getSystemClipboard();
    final ClipboardContent content = new ClipboardContent();
    content.putString(String.valueOf(toCopy));
    clipboard.setContent(content);
  }

}
