package com.faforever.client.domain;

import com.faforever.commons.api.dto.Faction;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class GamePlayerStatsBean {
  @EqualsAndHashCode.Include
  @ToString.Include
  ObjectProperty<PlayerBean> player = new SimpleObjectProperty<>();
  IntegerProperty score = new SimpleIntegerProperty();
  IntegerProperty team = new SimpleIntegerProperty();
  ObjectProperty<Faction> faction = new SimpleObjectProperty<>();
  ObjectProperty<OffsetDateTime> scoreTime = new SimpleObjectProperty<>();
  @EqualsAndHashCode.Include
  @ToString.Include
  ObjectProperty<ReplayBean> game = new SimpleObjectProperty<>();
  ObservableList<LeaderboardRatingJournalBean> leaderboardRatingJournals = FXCollections.observableArrayList();

  public PlayerBean getPlayer() {
    return player.get();
  }

  public ObjectProperty<PlayerBean> playerProperty() {
    return player;
  }

  public void setPlayer(PlayerBean player) {
    this.player.set(player);
  }

  public int getScore() {
    return score.get();
  }

  public IntegerProperty scoreProperty() {
    return score;
  }

  public void setScore(int score) {
    this.score.set(score);
  }

  public Faction getFaction() {
    return faction.get();
  }

  public ObjectProperty<Faction> factionProperty() {
    return faction;
  }

  public void setFaction(Faction faction) {
    this.faction.set(faction);
  }

  public int getTeam() {
    return team.get();
  }

  public IntegerProperty teamProperty() {
    return team;
  }

  public void setTeam(int team) {
    this.team.set(team);
  }

  public OffsetDateTime getScoreTime() {
    return scoreTime.get();
  }

  public ObjectProperty<OffsetDateTime> scoreTimeProperty() {
    return scoreTime;
  }

  public void setScoreTime(OffsetDateTime scoreTime) {
    this.scoreTime.set(scoreTime);
  }

  public ReplayBean getGame() {
    return game.get();
  }

  public ObjectProperty<ReplayBean> gameProperty() {
    return game;
  }

  public void setGame(ReplayBean game) {
    this.game.set(game);
  }

  public ObservableList<LeaderboardRatingJournalBean> getLeaderboardRatingJournals() {
    return leaderboardRatingJournals;
  }

  public void setLeaderboardRatingJournals(List<LeaderboardRatingJournalBean> leaderboardRatingJournals) {
    if (leaderboardRatingJournals == null) {
      leaderboardRatingJournals = List.of();
    }
    this.leaderboardRatingJournals.setAll(leaderboardRatingJournals);
  }
}
