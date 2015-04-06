package com.faforever.client.player;

import com.faforever.client.legacy.message.OnPlayerInfoMessageListener;

public interface PlayerService {

  void addOnPlayerInfoListener(OnPlayerInfoMessageListener listener);
}
