package com.faforever.client.leaderboard;

import com.faforever.client.api.Ranked1v1Stats;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.faforever.client.task.CompletableTask.Priority.HIGH;


@Lazy
@Service
@Profile("local")
public class MockLeaderboardService implements LeaderboardService {

  private final TaskService taskService;
  private final I18n i18n;

  @Inject
  public MockLeaderboardService(TaskService taskService, I18n i18n) {
    this.taskService = taskService;
    this.i18n = i18n;
  }

  @Override
  public CompletionStage<Ranked1v1Stats> getRanked1v1Stats() {
    return CompletableFuture.completedFuture(new Ranked1v1Stats());
  }

  @Override
  public CompletionStage<Ranked1v1EntryBean> getEntryForPlayer(int playerId) {
    return CompletableFuture.completedFuture(createLadderInfoBean("Player #" + playerId, 111, 222, 333, 55.55f));
  }

  @Override
  public CompletionStage<List<Ranked1v1EntryBean>> getEntries(KnownFeaturedMod ratingType) {
    return taskService.submitTask(new CompletableTask<List<Ranked1v1EntryBean>>(HIGH) {
      @Override
      protected List<Ranked1v1EntryBean> call() throws Exception {
        updateTitle("Reading ladder");

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
    }).getFuture();
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
