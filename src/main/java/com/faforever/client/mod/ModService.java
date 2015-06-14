package com.faforever.client.mod;

import com.faforever.client.game.GameTypeBean;
import com.faforever.client.legacy.OnGameTypeInfoListener;
import javafx.collections.ObservableList;

public interface ModService {

  void addOnModInfoListener(OnGameTypeInfoListener onGameTypeInfoListener);

  ObservableList<GameTypeBean> getInstalledMods();
}
