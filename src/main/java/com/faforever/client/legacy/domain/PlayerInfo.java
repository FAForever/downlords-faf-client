package com.faforever.client.legacy.domain;

import java.util.Map;

public class PlayerInfo extends ServerObject {

  public String clan;
  public String login;
  public Avatar avatar;
  public String country;
  public Float ratingMean;
  public Integer numberOfGames;
  public Float ratingDeviation;
  public Double ladderRatingMean;
  public Map<String, String> league;
  public Double ladderRatingDeviation;
}
