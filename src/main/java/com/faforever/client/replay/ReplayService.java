package com.faforever.client.replay;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

public interface ReplayService {

  Collection<ReplayInfoBean> getLocalReplays() throws IOException;

  Collection<ReplayInfoBean> getOnlineReplays();

  void runReplay(ReplayInfoBean item);

  void runLiveReplay(URI uri) throws URISyntaxException, IOException;
}
