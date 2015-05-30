package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.legacy.OnPlayerInfoListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;

public interface PlayerService {

  /**
   * Returns the PlayerInfoBean for the specified username. Returns null if no such player is known.
   */
  PlayerInfoBean getPlayerForUsername(String username);

  void addPlayerListener(MapChangeListener<String, PlayerInfoBean> stringPlayerInfoBeanMapChangeListener);

  PlayerInfoBean registerAndGetPlayerForUsername(String username);
}
