package com.faforever.client.lobby;

import com.faforever.client.legacy.OnLobbyConnectedListener;
import com.faforever.client.legacy.OnLobbyConnectingListener;
import com.faforever.client.legacy.OnFafDisconnectedListener;

public interface LobbyService {

  void setOnFafConnectedListener(OnLobbyConnectedListener listener);

  void setOnLobbyConnectingListener(OnLobbyConnectingListener listener);

  void setOnFafDisconnectedListener(OnFafDisconnectedListener listener);
}
