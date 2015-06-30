package com.faforever.client.mod;

import com.faforever.client.legacy.OnGameTypeInfoListener;
import com.faforever.client.util.Callback;

import java.util.List;

public interface ModService {

  void addOnModInfoListener(OnGameTypeInfoListener onGameTypeInfoListener);

  void getInstalledModsInBackground(Callback<List<ModInfoBean>> callback);
}
