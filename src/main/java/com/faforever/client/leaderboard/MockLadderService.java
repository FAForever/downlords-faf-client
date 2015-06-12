package com.faforever.client.leaderboard;

import com.faforever.client.i18n.I18n;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Callback;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static com.faforever.client.task.PrioritizedTask.Priority.HIGH;
import static com.faforever.client.task.TaskGroup.NET_LIGHT;

public class MockLadderService implements LadderService {

  @Autowired
  TaskService taskService;

  @Autowired
  I18n i18n;

  @Override
  public void getLadderInfo(Callback<List<LadderEntryBean>> callback) {
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<List<LadderEntryBean>>(i18n.get("readLadderTask.title"), HIGH) {
      @Override
      protected List<LadderEntryBean> call() throws Exception {
        ArrayList<LadderEntryBean> list = new ArrayList<LadderEntryBean>();
        for (int i = 1; i <= 10000; i++) {
          String name = RandomStringUtils.random(10);
          int rank = i;
          int rating = (int) (Math.random() * 2500);
          int gamecount = (int) (Math.random() * 10000);
          int score = (int) (Math.random() * 100);
          float winloss = (float) (Math.random() * 100);
          String division = RandomStringUtils.random(10);

          list.add(createLadderInfoBean(name, rank, rating, gamecount, score, winloss, division));

        }
        return list;
      }
    }, callback);
  }


  private LadderEntryBean createLadderInfoBean(String name, int rank, int rating, int gamesPlayed, int score, float winLossRatio, String division) {
    LadderEntryBean ladderEntryBean = new LadderEntryBean();
    ladderEntryBean.setUsername(name);
    ladderEntryBean.setRank(rank);
    ladderEntryBean.setRating(rating);
    ladderEntryBean.setGamesPlayed(gamesPlayed);
    ladderEntryBean.setScore(score);
    ladderEntryBean.setWinLossRatio(winLossRatio);
    ladderEntryBean.setDivision(division);

    return ladderEntryBean;
  }
}
