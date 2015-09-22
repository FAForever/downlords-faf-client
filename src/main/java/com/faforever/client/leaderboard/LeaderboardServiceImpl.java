package com.faforever.client.leaderboard;

import com.faforever.client.legacy.LobbyServerAccessor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LeaderboardServiceImpl implements LeaderboardService {

  @Autowired
  LobbyServerAccessor lobbyServerAccessor;

  @Override
  public CompletableFuture<List<LeaderboardEntryBean>> getLadderInfo() {
    return lobbyServerAccessor.requestLadderInfoInBackground();
  }
}
