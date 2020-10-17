package com.faforever.client.game;

import com.faforever.client.player.Player;
import com.faforever.client.util.RatingUtil;

import java.util.Optional;
import java.util.function.Predicate;

public class GameRangePredicate implements Predicate<Game> {
  private final Optional<Player> player;

  public GameRangePredicate(Optional<Player> player) {
    this.player = player;
  }

  @Override
  public boolean test(Game game) {
    if (!player.isPresent() || !game.getEnforceRating()) {
      return true;
    }

    float rating = RatingUtil.getGlobalRating(player.get());

    Integer maxRating = game.getMaxRating();

    if (maxRating != null && maxRating < rating) {
      return false;
    }

    Integer minRating = game.getMinRating();
    return minRating == null || rating >= minRating;
  }
}