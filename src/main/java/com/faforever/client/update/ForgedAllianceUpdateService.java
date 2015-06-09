package com.faforever.client.update;

import com.faforever.client.util.Callback;
import javafx.concurrent.Service;

public interface ForgedAllianceUpdateService {

  Service<Void> updateInBackground(String modName, Callback<Void> callback);
}
