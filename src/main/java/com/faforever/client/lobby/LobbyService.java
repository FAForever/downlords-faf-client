package com.faforever.client.lobby;

import com.faforever.client.legacy.OnFafDisconnectedListener;
import com.faforever.client.legacy.OnLobbyConnectedListener;
import com.faforever.client.legacy.OnLobbyConnectingListener;

public interface LobbyService {

  void setOnFafConnectedListener(OnLobbyConnectedListener listener);

  void setOnLobbyConnectingListener(OnLobbyConnectingListener listener);

  void setOnFafDisconnectedListener(OnFafDisconnectedListener listener);
}
