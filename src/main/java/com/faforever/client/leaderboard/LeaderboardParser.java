package com.faforever.client.leaderboard;

import java.io.IOException;
import java.util.List;

public interface LeaderboardParser {

  /**
   * Accesses the leaderboard over its HTML/AJAX thingy and parses the result. Since this isn't an API it's in no way
   * stable and this code breaks whenever the returned HTML changes.
   */
  List<LeaderboardEntryBean> parseLeaderboard() throws IOException;
}
