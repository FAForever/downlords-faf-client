package com.faforever.client.rating;

import com.faforever.client.domain.api.Replay;

public interface RatingService {
  /**
   * Calculates the game quality of the specified replay based in the "before" ratings its player stats.
   */
  double calculateQuality(Replay replay);
}
