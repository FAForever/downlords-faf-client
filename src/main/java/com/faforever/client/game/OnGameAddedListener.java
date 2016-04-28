package com.faforever.client.game;

import com.faforever.client.remote.domain.GameInfoMessage;

public interface OnGameAddedListener {

  void onGameAdded(GameInfoMessage gameInfoMessage);
}
