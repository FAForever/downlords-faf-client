package com.faforever.client.domain;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Value
public class LeagueEntryBean extends AbstractEntityBean<LeagueEntryBean> {
  @ToString.Include
  ObjectProperty<PlayerBean> player = new SimpleObjectProperty<>();
  IntegerProperty gamesPlayed = new SimpleIntegerProperty();
  IntegerProperty score = new SimpleIntegerProperty();
  BooleanProperty returningPlayer = new SimpleBooleanProperty();
  ObjectProperty<LeagueSeasonBean> leagueSeason = new SimpleObjectProperty<>();
  ObjectProperty<SubdivisionBean> subdivision = new SimpleObjectProperty<>();
  // This doesn't get set by the api, but we set it dynamically because it is dependent on how many other entries there are.
  IntegerProperty rank = new SimpleIntegerProperty();

  public PlayerBean getPlayer() {
    return player.get();
  }

  public void setPlayer(PlayerBean player) {
    this.player.set(player);
  }

  public ObjectProperty<PlayerBean> playerProperty() {
    return player;
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

  public int getScore() {
    return score.get();
  }

  public void setScore(int score) {
    this.score.set(score);
  }

  public IntegerProperty scoreProperty() {
    return score;
  }

  public boolean isReturningPlayer() {
    return returningPlayer.get();
  }

  public void setReturningPlayer(boolean returningPlayer) {
    this.returningPlayer.set(returningPlayer);
  }

  public BooleanProperty returningPlayerProperty() {
    return returningPlayer;
  }

  public LeagueSeasonBean getLeagueSeason() {
    return leagueSeason.get();
  }

  public void setLeagueSeason(LeagueSeasonBean leagueSeason) {
    this.leagueSeason.set(leagueSeason);
  }

  public ObjectProperty<LeagueSeasonBean> leagueSeasonProperty() {
    return leagueSeason;
  }

  public SubdivisionBean getSubdivision() {
    return subdivision.get();
  }

  public void setSubdivision(SubdivisionBean subdivision) {
    this.subdivision.set(subdivision);
  }

  public ObjectProperty<SubdivisionBean> subdivisionProperty() {
    return subdivision;
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
}
