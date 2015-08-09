package com.faforever.client.replay;

import com.faforever.client.util.Callback;

import java.util.List;

public interface ReplayServerAccessor {

  void requestOnlineReplays(Callback<List<ReplayInfoBean>> callback);
}
