package com.faforever.client.domain;

import com.faforever.client.replay.ReplayDetails;
import com.faforever.client.util.RatingUtil;
import com.faforever.commons.api.dto.Validity;

import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;

public record ReplayBean(
    Integer id,
    String title,
    boolean replayAvailable,
    Map<String, List<String>> teams,
    Map<String, List<GamePlayerStatsBean>> teamPlayerStats,
    PlayerBean host,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    FeaturedModBean featuredMod,
    MapVersionBean mapVersion,
    Path replayFile,
    Integer replayTicks,
    List<ChatMessage> chatMessages,
    List<GameOption> gameOptions,
    Validity validity,
    ReplayReviewsSummaryBean gameReviewsSummary,
    boolean local
) {

  public ReplayBean {
    teams = teams == null ? Map.of() : Map.copyOf(teams);
    teamPlayerStats = teamPlayerStats == null ? Map.of() : Map.copyOf(teamPlayerStats);
    chatMessages = chatMessages == null ? List.of() : List.copyOf(chatMessages);
    gameOptions = gameOptions == null ? List.of() : List.copyOf(gameOptions);
  }

  public int numPlayers() {
    return teams().values().stream().mapToInt(Collection::size).sum();
  }

  public double averageRating() {
    return teamPlayerStats.values()
                          .stream()
                          .flatMap(Collection::stream)
                          .map(GamePlayerStatsBean::leaderboardRatingJournals)
                          .filter(ratingJournals -> !ratingJournals.isEmpty())
                          .map(SequencedCollection::getFirst)
                          .mapToInt(ratingJournal -> RatingUtil.getRating(ratingJournal.meanBefore(),
                                                                          ratingJournal.deviationBefore()))
                          .average()
                          .orElse(Double.NaN);
  }

  public ReplayBean withReplayDetails(ReplayDetails replayDetails, Path replayFile) {
    return new ReplayBean(id(), title(), replayAvailable(), teams(), teamPlayerStats(), host(), startTime(), endTime(),
                          featuredMod(), replayDetails.mapVersion(), replayFile, replayTicks(),
                          replayDetails.chatMessages(), replayDetails.gameOptions(), validity(), gameReviewsSummary(),
                          local());
  }

  public record ChatMessage(Duration time, String sender, String message) {}

  public record GameOption(String key, String value) {}
}
