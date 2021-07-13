package com.faforever.client.game;

import com.faforever.client.remote.domain.inbound.faf.GameInfoMessage;

public interface OnGameAddedListener {

  void onGameAdded(GameInfoMessage gameInfoMessage);
}
