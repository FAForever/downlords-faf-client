package com.faforever.client.lobby;

import com.faforever.client.legacy.OnLobbyConnectedListener;
import com.faforever.client.legacy.OnLobbyConnectingListener;
import com.faforever.client.legacy.OnLobbyDisconnectedListener;

public interface LobbyService {

  void setOnFafConnectedListener(OnLobbyConnectedListener listener);

  void setOnLobbyConnectingListener(OnLobbyConnectingListener listener);

  void setOnLobbyDisconnectedListener(OnLobbyDisconnectedListener listener);
}
