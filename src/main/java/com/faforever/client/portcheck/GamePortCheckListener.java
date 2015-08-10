package com.faforever.client.portcheck;

public interface GamePortCheckListener {

  void onGamePortCheckResult(Boolean result);

  void onGamePortCheckStarted();
}
