package com.faforever.client.domain;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
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
  @EqualsAndHashCode.Include
  ObjectProperty<PlayerBean> player = new SimpleObjectProperty<>();
  IntegerProperty gamesPlayed = new SimpleIntegerProperty();
  IntegerProperty score = new SimpleIntegerProperty();
  ObjectProperty<LeagueSeasonBean> leagueSeason = new SimpleObjectProperty<>();
  ObjectProperty<SubdivisionBean> subdivision = new SimpleObjectProperty<>();

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
}
