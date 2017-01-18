package com.faforever.client.fx;

import java.nio.file.Path;

public interface PlatformService {

  void showDocument(String url);

  void reveal(Path path);

  void focusWindow(String windowTitle);

  void focusGameWindow();

  void startFlashingWindow(String windowTitle);

  void stopFlashingWindow(String windowTitle);

  void startFlashingGameWindow();

  void stopFlashingGameWindow();

  String getForegroundWindowTitle();

  boolean isGameWindowFocused();
}
