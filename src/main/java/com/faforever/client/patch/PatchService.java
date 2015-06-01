package com.faforever.client.patch;

import com.faforever.client.util.Callback;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public interface PatchService {

  Service<Void> patchInBackground(Callback<Void> callback);

  void needsPatching(Callback<Boolean> callback);
}
