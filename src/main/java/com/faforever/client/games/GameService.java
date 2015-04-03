package com.faforever.client.games;

import com.faforever.client.legacy.OnGameInfoListener;

public interface GameService {

  void publishPotentialPlayer();

  void addOnGameInfoListener(OnGameInfoListener listener);
}
