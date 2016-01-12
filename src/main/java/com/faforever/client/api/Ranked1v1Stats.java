package com.faforever.client.api;

import com.google.api.client.util.Key;

import java.util.Map;

public class Ranked1v1Stats {

  @Key
  private String id;

  @Key("rating_distribution")
  private Map<String, Integer> ratingDistribution;

  public Map<String, Integer> getRatingDistribution() {
    return ratingDistribution;
  }

  public void setRatingDistribution(Map<String, Integer> ratingDistribution) {
    this.ratingDistribution = ratingDistribution;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Ranked1v1Stats that = (Ranked1v1Stats) o;

    return !(id != null ? !id.equals(that.id) : that.id != null);

  }
}
