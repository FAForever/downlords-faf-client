package com.faforever.client.portcheck;

public interface PortCheckService {

  void checkGamePortInBackground();

  void addGamePortCheckListener(GamePortCheckListener listener);
}
