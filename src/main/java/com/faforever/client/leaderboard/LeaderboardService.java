package com.faforever.client.leaderboard;

import com.faforever.client.api.Ranked1v1Stats;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface LeaderboardService {

  CompletionStage<List<Ranked1v1EntryBean>> getRanked1v1Entries();

  CompletionStage<Ranked1v1Stats> getRanked1v1Stats();

  CompletionStage<Ranked1v1EntryBean> getEntryForPlayer(int playerId);
}
