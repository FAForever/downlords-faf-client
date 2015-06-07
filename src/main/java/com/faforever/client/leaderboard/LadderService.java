package com.faforever.client.leaderboard;

import com.faforever.client.util.Callback;

import java.util.List;

public interface LadderService {

  void getLadderInfo(Callback<List<LadderEntryBean>> callback);

}
