package com.faforever.client.update;

import com.faforever.client.game.ModInfoBean;
import com.faforever.client.util.Callback;
import javafx.concurrent.Service;

public interface UpdateService {

  Service<Void> updateInBackground(ModInfoBean modInfoBean, Callback<Void> callback);
}
