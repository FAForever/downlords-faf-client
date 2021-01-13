package com.faforever.client.domain;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Value
public class LeagueSeasonBean extends AbstractEntityBean<LeagueSeasonBean> {
  ObjectProperty<LeagueBean> league = new SimpleObjectProperty<>();
  ObjectProperty<LeaderboardBean> leaderboard = new SimpleObjectProperty<>();
  @ToString.Include
  StringProperty nameKey = new SimpleStringProperty();
  ObjectProperty<OffsetDateTime> startDate = new SimpleObjectProperty<>();
  ObjectProperty<OffsetDateTime> endDate = new SimpleObjectProperty<>();

  public LeagueBean getLeague() {
    return league.get();
  }

  public void setLeague(LeagueBean league) {
    this.league.set(league);
  }

  public ObjectProperty<LeagueBean> leagueProperty() {
    return league;
  }

  public LeaderboardBean getLeaderboard() {
    return leaderboard.get();
  }

  public void setLeaderboard(LeaderboardBean leaderboard) {
    this.leaderboard.set(leaderboard);
  }

  public ObjectProperty<LeaderboardBean> leaderboardProperty() {
    return leaderboard;
  }

  public String getNameKey() {
    return nameKey.get();
  }

  public void setNameKey(String nameKey) {
    this.nameKey.set(nameKey);
  }

  public StringProperty nameKeyProperty() {
    return nameKey;
  }

  public OffsetDateTime getStartDate() {
    return startDate.get();
  }

  public void setStartDate(OffsetDateTime startDate) {
    this.startDate.set(startDate);
  }

  public ObjectProperty<OffsetDateTime> startDateProperty() {
    return startDate;
  }

  public OffsetDateTime getEndDate() {
    return endDate.get();
  }

  public void setEndDate(OffsetDateTime endDate) {
    this.endDate.set(endDate);
  }

  public ObjectProperty<OffsetDateTime> endDateProperty() {
    return endDate;
  }
}
