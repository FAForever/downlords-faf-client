package com.faforever.client.replay;

import com.faforever.client.util.Callback;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;

public interface ReplayService {

  Collection<ReplayInfoBean> getLocalReplays() throws IOException;

  void getOnlineReplays(Callback<List<ReplayInfoBean>> callback);

  void runReplay(ReplayInfoBean item);

  void runLiveReplay(URI uri) throws IOException;
}
