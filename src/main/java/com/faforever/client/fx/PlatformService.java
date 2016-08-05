package com.faforever.client.fx;

import java.nio.file.Path;

public interface PlatformService {

  void showDocument(String url);

  void reveal(Path path);

  void showWindow(String windowTitle);

  void startFlashingWindow(String windowTitle);

  void stopFlashingWindow(String windowTitle);

  String getForegroundWindowTitle();
}
