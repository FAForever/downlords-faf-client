package com.faforever.client.leaderboard;

import com.faforever.client.api.LeaderboardEntry;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Ranked1v1EntryBean {

  private StringProperty username;
  private IntegerProperty rank;
  private IntegerProperty rating;
  private IntegerProperty gamesPlayed;
  private FloatProperty winLossRatio;

  public Ranked1v1EntryBean() {
    username = new SimpleStringProperty();
    rank = new SimpleIntegerProperty();
    rating = new SimpleIntegerProperty();
    gamesPlayed = new SimpleIntegerProperty();
    winLossRatio = new SimpleFloatProperty();
  }

  public String getUsername() {
    return username.get();
  }

  public void setUsername(String username) {
    this.username.set(username);
  }

  public StringProperty usernameProperty() {
    return username;
  }

  public int getRank() {
    return rank.get();
  }

  public void setRank(int rank) {
    this.rank.set(rank);
  }

  public IntegerProperty rankProperty() {
    return rank;
  }

  public int getRating() {
    return rating.get();
  }

  public void setRating(int rating) {
    this.rating.set(rating);
  }

  public IntegerProperty ratingProperty() {
    return rating;
  }

  public int getGamesPlayed() {
    return gamesPlayed.get();
  }

  public void setGamesPlayed(int gamesPlayed) {
    this.gamesPlayed.set(gamesPlayed);
  }

  public IntegerProperty gamesPlayedProperty() {
    return gamesPlayed;
  }

  public float getWinLossRatio() {
    return winLossRatio.get();
  }

  public void setWinLossRatio(float winLossRatio) {
    this.winLossRatio.set(winLossRatio);
  }

  public FloatProperty winLossRatioProperty() {
    return winLossRatio;
  }

  @Override
  public int hashCode() {
    return username.get() != null ? username.get().hashCode() : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Ranked1v1EntryBean that = (Ranked1v1EntryBean) o;

    return !(username.get() != null ? !username.get().equals(that.username.get()) : that.username.get() != null);

  }

  @Override
  public String toString() {
    return "Ranked1v1EntryBean{" +
        "username=" + username.get() +
        '}';
  }

  public static Ranked1v1EntryBean fromLeaderboardEntry(LeaderboardEntry leaderboardEntry) {
    Ranked1v1EntryBean ranked1v1EntryBean = new Ranked1v1EntryBean();
    ranked1v1EntryBean.setUsername(leaderboardEntry.getLogin());
    ranked1v1EntryBean.setGamesPlayed(leaderboardEntry.getNumGames());
    ranked1v1EntryBean.setRank(leaderboardEntry.getRanking());
    ranked1v1EntryBean.setRating(leaderboardEntry.getRating());
    ranked1v1EntryBean.setWinLossRatio(leaderboardEntry.getWonGames() / (float) leaderboardEntry.getNumGames());
    return ranked1v1EntryBean;
  }
}
