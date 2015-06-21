package com.faforever.client.mod;

import com.faforever.client.game.GameTypeBean;
import com.faforever.client.legacy.OnGameTypeInfoListener;
import com.faforever.client.legacy.LobbyServerAccessor;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.springframework.beans.factory.annotation.Autowired;

public class ModServiceImpl implements ModService {

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;

  @Override
  public void addOnModInfoListener(OnGameTypeInfoListener onGameTypeInfoListener) {
    lobbyServerAccessor.addOnGameTypeInfoListener(onGameTypeInfoListener);
  }

  @Override
  public ObservableList<GameTypeBean> getInstalledMods() {
    return FXCollections.emptyObservableList();
  }

}
