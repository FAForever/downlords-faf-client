package com.faforever.client.io;

public interface ProgressListener {

  void update(long workDone, long totalWork);
}
