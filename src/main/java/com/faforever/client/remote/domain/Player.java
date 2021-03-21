package com.faforever.client.remote.domain;

import java.util.Map;

/**
 * A player info as received from the FAF server. The FAF server sends it as JSON string which is deserialized into an
 * instance of this class.
 */
public class Player {

  private int id;
  private String clan;
  private String login;
  private Avatar avatar;
  private String country;
  private Integer numberOfGames;
  private Map<String, String> league;
  private Map<String, LeaderboardRating> ratings;

  public Map<String, LeaderboardRating> getRatings() {
    return ratings;
  }

  public void setRatings(Map<String, LeaderboardRating> ratings) {
    this.ratings = ratings;
  }

  public String getClan() {
    return clan;
  }

  public void setClan(String clan) {
    this.clan = clan;
  }

  public String getLogin() {
    return login;
  }

  public void setLogin(String login) {
    this.login = login;
  }

  public Avatar getAvatar() {
    return avatar;
  }

  public void setAvatar(Avatar avatar) {
    this.avatar = avatar;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public Integer getNumberOfGames() {
    return numberOfGames;
  }

  public void setNumberOfGames(Integer numberOfGames) {
    this.numberOfGames = numberOfGames;
  }

  public Map<String, String> getLeague() {
    return league;
  }

  public void setLeague(Map<String, String> league) {
    this.league = league;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }
}
