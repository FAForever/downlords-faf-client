package com.faforever.client.lobby;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnFafDisconnectedListener;
import com.faforever.client.legacy.OnLobbyConnectedListener;
import com.faforever.client.legacy.OnLobbyConnectingListener;
import org.springframework.beans.factory.annotation.Autowired;

public class LobbyServiceImpl implements LobbyService {

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;

  @Override
  public void setOnFafConnectedListener(OnLobbyConnectedListener listener) {
    lobbyServerAccessor.setOnLobbyConnectedListener(listener);
  }

  @Override
  public void setOnLobbyConnectingListener(OnLobbyConnectingListener listener) {
    lobbyServerAccessor.setOnFafConnectingListener(listener);
  }

  @Override
  public void setOnFafDisconnectedListener(OnFafDisconnectedListener listener) {
    lobbyServerAccessor.setOnFafDisconnectedListener(listener);
  }
}
