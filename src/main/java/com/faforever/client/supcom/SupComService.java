package com.faforever.client.supcom;

import com.faforever.client.util.Callback;

import java.util.List;

public interface SupComService {
  void startGame(int uid, String mod, List<String> additionalCommandLine, Callback<Void> callback);
}
