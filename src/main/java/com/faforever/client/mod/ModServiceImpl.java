package com.faforever.client.mod;

import com.faforever.client.game.GameTypeBean;
import com.faforever.client.legacy.OnGameTypeInfoListener;
import com.faforever.client.legacy.ServerAccessor;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.springframework.beans.factory.annotation.Autowired;

public class ModServiceImpl implements ModService {

  @Autowired
  ServerAccessor serverAccessor;

  @Override
  public void addOnModInfoListener(OnGameTypeInfoListener onGameTypeInfoListener) {
    serverAccessor.addOnGameTypeInfoListener(onGameTypeInfoListener);
  }

  @Override
  public ObservableList<GameTypeBean> getInstalledMods() {
    return FXCollections.emptyObservableList();
  }

}
