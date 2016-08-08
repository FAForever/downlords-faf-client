package com.faforever.client.io;

public interface ByteCountListener {

  void updateBytesWritten(long written, long total);
}
