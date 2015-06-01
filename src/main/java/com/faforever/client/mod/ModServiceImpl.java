package com.faforever.client.mod;

import com.faforever.client.legacy.OnModInfoListener;
import com.faforever.client.legacy.ServerAccessor;
import org.springframework.beans.factory.annotation.Autowired;

public class ModServiceImpl implements ModService {

  @Autowired
  ServerAccessor serverAccessor;

  @Override
  public void addOnModInfoListener(OnModInfoListener onModInfoListener) {
    serverAccessor.addOnModInfoMessageListener(onModInfoListener);
  }
}
