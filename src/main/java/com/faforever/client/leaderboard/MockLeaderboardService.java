package com.faforever.client.leaderboard;

import com.faforever.client.i18n.I18n;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.TaskService;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.task.AbstractPrioritizedTask.Priority.HIGH;

public class MockLeaderboardService implements LeaderboardService {

  @Resource
  TaskService taskService;

  @Resource
  I18n i18n;

  @Override
  public CompletableFuture<List<LeaderboardEntryBean>> getLeaderboardEntries() {
    return taskService.submitTask(new AbstractPrioritizedTask<List<LeaderboardEntryBean>>(HIGH) {
      @Override
      protected List<LeaderboardEntryBean> call() throws Exception {
        updateTitle(i18n.get("readLadderTask.title"));

        List<LeaderboardEntryBean> list = new ArrayList<>();
        for (int i = 1; i <= 10000; i++) {
          String name = RandomStringUtils.random(10);
          int rating = (int) (Math.random() * 2500);
          int gamecount = (int) (Math.random() * 10000);
          int score = (int) (Math.random() * 100);
          float winloss = (float) (Math.random() * 100);
          String division = RandomStringUtils.random(10);

          list.add(createLadderInfoBean(name, i, rating, gamecount, winloss));

        }
        return list;
      }
    });
  }

  @Override
  public CompletableFuture<List<RatingDistribution>> getRatingDistributions() {
    return CompletableFuture.completedFuture(Collections.<RatingDistribution>emptyList());
  }

  @Override
  public CompletableFuture<LeaderboardEntryBean> getEntryForPlayer(String username) {
    return CompletableFuture.completedFuture(createLadderInfoBean(username, 111, 222, 333, 55.55f));
  }


  private LeaderboardEntryBean createLadderInfoBean(String name, int rank, int rating, int gamesPlayed, float winLossRatio) {
    LeaderboardEntryBean leaderboardEntryBean = new LeaderboardEntryBean();
    leaderboardEntryBean.setUsername(name);
    leaderboardEntryBean.setRank(rank);
    leaderboardEntryBean.setRating(rating);
    leaderboardEntryBean.setGamesPlayed(gamesPlayed);
    leaderboardEntryBean.setWinLossRatio(winLossRatio);

    return leaderboardEntryBean;
  }
}
