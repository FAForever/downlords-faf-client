package com.faforever.client.relay.event;

import com.faforever.client.game.GameInfoBean;

public class GameFullEvent {
  private final GameInfoBean gameInfoBean;

  public GameFullEvent(GameInfoBean gameInfoBean) {
    this.gameInfoBean = gameInfoBean;
  }

  public GameInfoBean getGameInfoBean() {
    return gameInfoBean;
  }
}
