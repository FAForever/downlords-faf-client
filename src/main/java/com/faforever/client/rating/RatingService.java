package com.faforever.client.rating;

import com.faforever.client.domain.ReplayBean;

public interface RatingService {
  /**
   * Calculates the game quality of the specified replay based in the "before" ratings its player stats.
   */
  double calculateQuality(ReplayBean replay);
}
