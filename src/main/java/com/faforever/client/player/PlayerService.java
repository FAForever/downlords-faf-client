package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import javafx.collections.MapChangeListener;

import java.util.Set;

public interface PlayerService {

  /**
   * Returns the PlayerInfoBean for the specified username. Returns null if no such player is known.
   */
  PlayerInfoBean getPlayerForUsername(String username);

  void addPlayerListener(MapChangeListener<String, PlayerInfoBean> stringPlayerInfoBeanMapChangeListener);

  PlayerInfoBean registerAndGetPlayerForUsername(String username);

  Set<String> getPlayerNames();
}
