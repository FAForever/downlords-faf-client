package com.faforever.client.leaderboard;

import com.faforever.client.api.dto.GlobalRatingWithRank;
import com.faforever.client.api.dto.Ladder1v1RatingWithRank;
import com.faforever.client.api.dto.Player;
import com.github.jasminb.jsonapi.annotations.Relationship;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Data;

public class RatingWithRank {
  private StringProperty id;
  private DoubleProperty mean;
  private DoubleProperty deviation;
  private DoubleProperty rating;
  private IntegerProperty rank;
  private ObjectProperty<Player> player;

  public RatingWithRank(){
    id = new SimpleStringProperty();
    mean = new SimpleDoubleProperty();
    deviation = new SimpleDoubleProperty();
    rating = new SimpleDoubleProperty();
    rank = new SimpleIntegerProperty();
    player = new SimpleObjectProperty<Player>();
  }

  public static RatingWithRank fromGlobal(GlobalRatingWithRank globalRatingWithRank) {
    RatingWithRank ratingWithRank = new RatingWithRank();
    ratingWithRank.setId(globalRatingWithRank.getId());
    ratingWithRank.setMean(globalRatingWithRank.getMean());
    ratingWithRank.setDeviation(globalRatingWithRank.getDeviation());
    ratingWithRank.setRating(globalRatingWithRank.getRating());
    ratingWithRank.setRank(globalRatingWithRank.getRank());
    ratingWithRank.setPlayer(globalRatingWithRank.getPlayer());
    return ratingWithRank;
  }

  public static RatingWithRank fromLadder1v1(Ladder1v1RatingWithRank ladder1v1RatingWithRank) {
    RatingWithRank ratingWithRank = new RatingWithRank();
    ratingWithRank.setId(ladder1v1RatingWithRank.getId());
    ratingWithRank.setMean(ladder1v1RatingWithRank.getMean());
    ratingWithRank.setDeviation(ladder1v1RatingWithRank.getDeviation());
    ratingWithRank.setRating(ladder1v1RatingWithRank.getRating());
    ratingWithRank.setRank(ladder1v1RatingWithRank.getRank());
    ratingWithRank.setPlayer(ladder1v1RatingWithRank.getPlayer());
    return ratingWithRank;
  }


  public StringProperty idProperty() {
    return id;
  }

  public String getId () {
    return id.get();
  }

  public void setId(String id) {
    this.id.set(id);
  }

  public DoubleProperty meanProperty() {
    return mean;
  }

  public double getMean () {
    return mean.get();
  }

  public void setMean(double mean) {
    this.mean.set(mean);
  }

  public DoubleProperty deviationProperty() {
    return deviation;
  }

  public double getDeviation () {
    return deviation.get();
  }

  public void setDeviation(double deviation) {
    this.deviation.set(deviation);
  }

  public DoubleProperty ratingProperty() {
    return rating;
  }

  public double getRating () {
    return rating.get();
  }

  public void setRating(double rating) {
    this.rating.set(rating);
  }

  public IntegerProperty rankProperty() {
    return rank;
  }

  public int getRank() {
    return rank.get();
  }

  public void setRank(int rank) {
    this.rank.set(rank);
  }

  public ObjectProperty<Player> playerProperty() {
    return player;
  }

  public Player getPlayer() {
    return player.get();
  }

  public void setPlayer(Player player) {
    this.player.set(player);
  }
}
