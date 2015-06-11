package com.faforever.client.patch;

import com.faforever.client.util.Callback;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public interface PatchService {

  void patchInBackground(Callback<Void> callback);

  void needsPatching(Callback<Boolean> callback);
}
