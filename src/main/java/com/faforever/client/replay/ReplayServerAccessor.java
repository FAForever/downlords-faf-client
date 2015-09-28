package com.faforever.client.replay;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ReplayServerAccessor {

  CompletableFuture<List<ReplayInfoBean>> requestOnlineReplays();
}
