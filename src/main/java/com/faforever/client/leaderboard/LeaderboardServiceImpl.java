package com.faforever.client.leaderboard;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class LeaderboardServiceImpl implements LeaderboardService {

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;

  @Override
  public void getLadderInfo(Callback<List<LeaderboardEntryBean>> callback) {
    lobbyServerAccessor.requestLadderInfoInBackground(callback);
  }
}
