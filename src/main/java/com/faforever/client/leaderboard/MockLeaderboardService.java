package com.faforever.client.leaderboard;

import com.faforever.client.FafClientApplication;
import com.faforever.client.i18n.I18n;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Lazy
@Service
@Profile(FafClientApplication.PROFILE_OFFLINE)
@RequiredArgsConstructor
public class MockLeaderboardService implements LeaderboardService {

  private final TaskService taskService;
  private final I18n i18n;

  public CompletableFuture<List<RatingStat>> getLeaderboardStats(String leaderboardTechnicalName) {
    return CompletableFuture.completedFuture(Collections.emptyList());
  }

  @Override
  public CompletableFuture<List<Leaderboard>> getLeaderboards() {
    return CompletableFuture.completedFuture(Collections.emptyList());
  }

  @Override
  public CompletableFuture<List<LeaderboardEntry>> getEntriesForPlayer(int playerId) {
    return CompletableFuture.completedFuture(Collections.emptyList());
  }

  @Override
  public CompletableFuture<List<LeaderboardEntry>> getEntries(Leaderboard leaderboard) {
    return CompletableFuture.completedFuture(Collections.emptyList());
  }

  @Override
  public CompletableFuture<Tuple<List<LeaderboardEntry>, Integer>> getPagedEntries(Leaderboard leaderboard, int count, int page) {
    return CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), 1));
  }
}
