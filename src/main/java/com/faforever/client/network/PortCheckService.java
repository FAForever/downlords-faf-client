package com.faforever.client.network;

public interface PortCheckService {

  void checkGamePortInBackground();

  void addGamePortCheckListener(GamePortCheckListener listener);
}
