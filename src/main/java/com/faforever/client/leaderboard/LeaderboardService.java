package com.faforever.client.leaderboard;

import com.faforever.client.util.Callback;

import java.util.List;

public interface LeaderboardService {

  void getLadderInfo(Callback<List<LeaderboardEntryBean>> callback);

}
