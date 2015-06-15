package com.faforever.client.lobby;

import com.faforever.client.legacy.OnLobbyConnectedListener;
import com.faforever.client.legacy.OnLobbyConnectingListener;
import com.faforever.client.legacy.OnFafDisconnectedListener;
import com.faforever.client.legacy.ServerAccessor;
import org.springframework.beans.factory.annotation.Autowired;

public class LobbyServiceImpl implements LobbyService {

  @Autowired
  ServerAccessor serverAccessor;

  @Override
  public void setOnFafConnectedListener(OnLobbyConnectedListener listener) {
    serverAccessor.setOnLobbyConnectedListener(listener);
  }

  @Override
  public void setOnLobbyConnectingListener(OnLobbyConnectingListener listener) {
    serverAccessor.setOnFafConnectingListener(listener);
  }

  @Override
  public void setOnFafDisconnectedListener(OnFafDisconnectedListener listener) {
    serverAccessor.setOnFafDisconnectedListener(listener);
  }
}
