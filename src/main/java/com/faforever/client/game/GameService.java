package com.faforever.client.game;

import com.faforever.client.legacy.OnGameInfoListener;
import com.faforever.client.util.Callback;

public interface GameService {

  void publishPotentialPlayer();

  void addOnGameInfoListener(OnGameInfoListener listener);

  void hostGame(NewGameInfo name, Callback<Void> callback);

  void joinGame(GameInfoBean gameInfoBean, Callback<Void> callback);
}
