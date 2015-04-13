package com.faforever.client.games;

import com.faforever.client.legacy.message.OnGameInfoMessageListener;
import com.faforever.client.util.Callback;

public interface GameService {

  void publishPotentialPlayer();

  void addOnGameInfoListener(OnGameInfoMessageListener listener);

  void hostGame(NewGameInfo name, Callback<Void> callback);
}
