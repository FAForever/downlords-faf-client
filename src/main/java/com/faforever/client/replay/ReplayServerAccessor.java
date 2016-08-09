package com.faforever.client.replay;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface ReplayServerAccessor {

  CompletionStage<List<ReplayInfoBean>> requestOnlineReplays();
}
