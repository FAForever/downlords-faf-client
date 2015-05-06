package com.faforever.client.player;

import com.faforever.client.legacy.OnPlayerInfoListener;
import com.faforever.client.legacy.ServerAccessor;
import org.springframework.beans.factory.annotation.Autowired;

public class PlayerServiceImpl implements PlayerService {

  @Autowired
  ServerAccessor serverAccessor;

  @Override
  public void addOnPlayerInfoListener(OnPlayerInfoListener listener) {
    serverAccessor.addOnPlayerInfoMessageListener(listener);
  }
}
