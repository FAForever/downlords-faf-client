package com.faforever.client.leaderboard;

import com.faforever.client.api.Ranked1v1Stats;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LeaderboardService {

  CompletableFuture<List<Ranked1v1EntryBean>> getRanked1v1Entries();

  CompletableFuture<Ranked1v1Stats> getRanked1v1Stats();

  CompletableFuture<Ranked1v1EntryBean> getEntryForPlayer(int playerId);
}
