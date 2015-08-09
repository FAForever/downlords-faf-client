package com.faforever.client.legacy.domain;

import java.util.Map;

/**
 * A player info as received from the FAF server. The FAF server sends it as JSON string which is deserialized into an
 * instance of this class.
 */
public class PlayerInfo extends ServerObject {

  public String clan;
  public String login;
  public Avatar avatar;
  public String country;
  public float ratingMean;
  public Integer numberOfGames;
  public float ratingDeviation;
  public Double ladderRatingMean;
  public Map<String, String> league;
  public Double ladderRatingDeviation;
}
