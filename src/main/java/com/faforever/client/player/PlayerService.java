package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.legacy.OnPlayerInfoListener;
import javafx.collections.ObservableMap;

public interface PlayerService {

  void addOnPlayerInfoListener(OnPlayerInfoListener listener);

  ObservableMap<String, PlayerInfoBean> getKnownPlayers();
}
