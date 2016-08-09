package com.faforever.client.replay;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ReplayService {

  Collection<ReplayInfoBean> getLocalReplays() throws IOException;

  CompletableFuture<List<ReplayInfoBean>> getOnlineReplays();

  void runReplay(ReplayInfoBean item);

  void runLiveReplay(int gameId, int playerId) throws IOException;

  void runLiveReplay(URI uri) throws IOException;

  void startReplayServer(int gameUid);
}
