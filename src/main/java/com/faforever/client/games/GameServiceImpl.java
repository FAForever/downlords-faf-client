package com.faforever.client.games;

import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.legacy.ServerAccessor;
import com.faforever.client.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;

public class GameServiceImpl implements GameService {

  @Autowired
  ServerAccessor serverAccessor;

  @Autowired
  UserService userService;

  @Override
  public void publishPotentialPlayer() {
    String username = userService.getUsername();
//    serverAccessor.publishPotentialPlayer(username);
  }

  @Override
  public void addOnGameInfoListener(OnGameInfoListener listener) {
    serverAccessor.addOnGameInfoListener(listener);
  }
}
