package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.Player;


public interface OnPlayerInfoListener {

  /**
   * This method is called whenever a player info from the FAF server has been received.
   */
  void onPlayerInfo(Player player);
}
