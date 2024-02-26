package com.faforever.client.domain;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class LeagueEntryBean extends AbstractEntityBean {
  @ToString.Include
  private final ObjectProperty<PlayerBean> player = new SimpleObjectProperty<>();
  private final IntegerProperty gamesPlayed = new SimpleIntegerProperty();
  private final ObjectProperty<Integer> score = new SimpleObjectProperty<>();
  private final BooleanProperty returningPlayer = new SimpleBooleanProperty();
  private final ObjectProperty<LeagueSeasonBean> leagueSeason = new SimpleObjectProperty<>();
  private final ObjectProperty<SubdivisionBean> subdivision = new SimpleObjectProperty<>();
  // This doesn't get set by the api, but we set it dynamically because it is dependent on how many other entries there are.
  private final IntegerProperty rank = new SimpleIntegerProperty();

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

  public Integer getScore() {
    return score.get();
  }

  public void setScore(Integer score) {
    this.score.set(score);
  }

  public ObjectProperty<Integer> scoreProperty() {
    return score;
  }

  public boolean getReturningPlayer() {
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
