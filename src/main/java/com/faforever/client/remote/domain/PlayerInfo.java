package com.faforever.client.remote.domain;

import lombok.Value;

import java.util.Map;

/**
 * A player info as received from the FAF server. The FAF server sends it as JSON string which is deserialized into an
 * instance of this class.
 */
@Value
public class PlayerInfo {
  int id;
  String login;
  String clan;
  Avatar avatar;
  String country;
  Integer numberOfGames;
  Map<String, String> league;
  Map<String, LeaderboardRating> ratings;
}
