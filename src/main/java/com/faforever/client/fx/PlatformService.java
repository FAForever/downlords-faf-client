package com.faforever.client.fx;

import java.nio.file.Path;

public interface PlatformService {

  void showDocument(String url);

  void reveal(Path path);

  void focusWindow(String windowTitle);

  void startFlashingWindow(String windowTitle);

  void stopFlashingWindow(String windowTitle);

  boolean isWindowFocused(String windowTitle);
}
