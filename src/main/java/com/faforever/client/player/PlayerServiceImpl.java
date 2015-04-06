package com.faforever.client.player;

import com.faforever.client.legacy.message.OnPlayerInfoMessageListener;
import com.faforever.client.legacy.ServerAccessor;
import org.springframework.beans.factory.annotation.Autowired;

public class PlayerServiceImpl implements PlayerService {

  @Autowired
  ServerAccessor serverAccessor;

  @Override
  public void addOnPlayerInfoListener(OnPlayerInfoMessageListener listener) {
    serverAccessor.addOnPlayerInfoMessageListener(listener);
  }
}
