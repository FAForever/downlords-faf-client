package com.faforever.client.leaderboard;

import com.faforever.client.FafClientApplication;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.task.CompletableTask.Priority.HIGH;


@Lazy
@Service
@Profile(FafClientApplication.PROFILE_OFFLINE)
@RequiredArgsConstructor
public class MockLeaderboardService implements LeaderboardService {

  private final TaskService taskService;
  private final I18n i18n;

  @Override
  public CompletableFuture<List<RatingStat>> getLadder1v1Stats() {
    return CompletableFuture.completedFuture(Collections.emptyList());
  }

  @Override
  public CompletableFuture<List<DivisionStat>> getDivisionStats() {
    return CompletableFuture.completedFuture(Collections.emptyList());
  }

  @Override
  public CompletableFuture<List<Division>> getDivisions() {
    return CompletableFuture.completedFuture(Collections.emptyList());
  }

  @Override
  public CompletableFuture<List<LeaderboardEntry>> getDivisionEntries(Division division) {
    return CompletableFuture.completedFuture(Collections.emptyList());
  }

  @Override
  public CompletableFuture<LeaderboardEntry> getEntryForPlayer(int playerId) {
    return CompletableFuture.completedFuture(createLadderInfoBean("Player #" + playerId, 111, 222, 333, 55.55f));
  }

  @Override
  public CompletableFuture<LeaderboardEntry> getLeagueEntryForPlayer(int playerId) {
    return null;
  }

  @Override
  public CompletableFuture<List<LeaderboardEntry>> getEntries(KnownFeaturedMod ratingType) {
    return taskService.submitTask(new CompletableTask<List<LeaderboardEntry>>(HIGH) {
      @Override
      protected List<LeaderboardEntry> call() throws Exception {
        updateTitle("Reading ladder");

        List<LeaderboardEntry> list = new ArrayList<>();
        for (int i = 1; i <= 10000; i++) {
          String name = RandomStringUtils.random(10);
          int rating = (int) (Math.random() * 2500);
          int gamecount = (int) (Math.random() * 10000);
          float winloss = (float) (Math.random() * 100);

          list.add(createLadderInfoBean(name, i, rating, gamecount, winloss));

        }
        return list;
      }
    }).getFuture();
  }

  private LeaderboardEntry createLadderInfoBean(String name, int rank, int rating, int gamesPlayed, float winLossRatio) {
    LeaderboardEntry leaderboardEntry = new LeaderboardEntry();
    leaderboardEntry.setUsername(name);
    leaderboardEntry.setRank(rank);
    leaderboardEntry.setRating(rating);
    leaderboardEntry.setGamesPlayed(gamesPlayed);
    leaderboardEntry.setWinLossRatio(winLossRatio);

    return leaderboardEntry;
  }
}
