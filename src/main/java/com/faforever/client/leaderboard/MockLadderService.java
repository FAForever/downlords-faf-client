package com.faforever.client.leaderboard;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockLadderService implements LadderService {


  @Override
  public List<LadderInfoBean> getLadderInfo() {
    ArrayList<LadderInfoBean> list = new ArrayList<LadderInfoBean>();
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


/*
    private StringProperty username;
    private IntegerProperty rank;
    private IntegerProperty rating;
    private IntegerProperty gamesPlayed;
    private FloatProperty score;
    private FloatProperty winLossRatio;
    private StringProperty division*/

  }


  private LadderInfoBean createLadderInfoBean(String name, int rank, int rating, int gamesPlayed, int score, float winLossRatio, String division) {
    LadderInfoBean ladderInfoBean = new LadderInfoBean();
    ladderInfoBean.setUsername(name);
    ladderInfoBean.setRank(rank);
    ladderInfoBean.setRating(rating);
    ladderInfoBean.setGamesPlayed(gamesPlayed);
    ladderInfoBean.setScore(score);
    ladderInfoBean.setWinLossRatio(winLossRatio);
    ladderInfoBean.setDivision(division);




    return ladderInfoBean;
  }

}
