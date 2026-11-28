package com.faforever.client.fa.relay.ice;

import java.util.HashMap;
import java.util.Map;

/**
 * What a game told the client it can come back to its match with: the game's own source, and the source of every
 * player by id. A game only says so when every player's engine can do it.
 */
public record RejoinOffer(int localSource, Map<Integer, Integer> sourceByPlayerId) {

  /**
   * @param localSource the game's own source
   * @param seats {@code playerId=source,...}
   * @throws IllegalArgumentException when either isn't in that form
   */
  public static RejoinOffer parse(String localSource, String seats) {
    Map<Integer, Integer> sources = new HashMap<>();
    try {
      for (String seat : seats.split(",")) {
        String[] parts = seat.split("=");
        if (parts.length != 2) {
          throw new IllegalArgumentException("Not a seat: '" + seat + "'");
        }
        sources.put(Integer.valueOf(parts[0].trim()), Integer.valueOf(parts[1].trim()));
      }
      return new RejoinOffer(Integer.parseInt(localSource.trim()), sources);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Not a rejoin offer: '" + localSource + "', '" + seats + "'", e);
    }
  }
}
