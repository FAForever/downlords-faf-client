package com.faforever.client.leaderboard;

import com.faforever.client.api.Ranked1v1Stats;
import com.faforever.client.i18n.I18n;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.task.TaskService;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.task.AbstractPrioritizedTask.Priority.HIGH;

public class MockLeaderboardService implements LeaderboardService {

  @Resource
  TaskService taskService;

  @Resource
  I18n i18n;

  @Override
  public CompletableFuture<List<Ranked1v1EntryBean>> getRanked1v1Entries() {
    return taskService.submitTask(new AbstractPrioritizedTask<List<Ranked1v1EntryBean>>(HIGH) {
      @Override
      protected List<Ranked1v1EntryBean> call() throws Exception {
        updateTitle(i18n.get("readLadderTask.title"));

        List<Ranked1v1EntryBean> list = new ArrayList<>();
        for (int i = 1; i <= 10000; i++) {
          String name = RandomStringUtils.random(10);
          int rating = (int) (Math.random() * 2500);
          int gamecount = (int) (Math.random() * 10000);
          float winloss = (float) (Math.random() * 100);

          list.add(createLadderInfoBean(name, i, rating, gamecount, winloss));

        }
        return list;
      }
    });
  }

  @Override
  public CompletableFuture<Ranked1v1Stats> getRanked1v1Stats() {
    return CompletableFuture.completedFuture(new Ranked1v1Stats());
  }

  @Override
  public CompletableFuture<Ranked1v1EntryBean> getEntryForPlayer(int playerId) {
    return CompletableFuture.completedFuture(createLadderInfoBean("Player #" + playerId, 111, 222, 333, 55.55f));
  }


  private Ranked1v1EntryBean createLadderInfoBean(String name, int rank, int rating, int gamesPlayed, float winLossRatio) {
    Ranked1v1EntryBean ranked1v1EntryBean = new Ranked1v1EntryBean();
    ranked1v1EntryBean.setUsername(name);
    ranked1v1EntryBean.setRank(rank);
    ranked1v1EntryBean.setRating(rating);
    ranked1v1EntryBean.setGamesPlayed(gamesPlayed);
    ranked1v1EntryBean.setWinLossRatio(winLossRatio);

    return ranked1v1EntryBean;
  }
}
