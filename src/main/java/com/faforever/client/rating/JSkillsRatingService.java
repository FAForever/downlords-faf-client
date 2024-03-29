package com.faforever.client.rating;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.TrueSkill;
import com.faforever.client.domain.api.GamePlayerStats;
import com.faforever.client.domain.api.Replay;
import jskills.GameInfo;
import jskills.Rating;
import jskills.Team;
import jskills.TrueSkillCalculator;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JSkillsRatingService implements RatingService {
  private final GameInfo gameInfo;

  public JSkillsRatingService(ClientProperties clientProperties) {
    TrueSkill trueSkill = clientProperties.getTrueSkill();
    gameInfo = new GameInfo(trueSkill.getInitialMean(), trueSkill.getInitialStandardDeviation(), trueSkill.getBeta(),
        trueSkill.getDynamicFactor(), trueSkill.getDrawProbability());
  }

  @Override
  public double calculateQuality(Replay replay) {
    Collection<List<GamePlayerStats>> teams = replay.teamPlayerStats().values();
    if (teams.size() != 2) {
      return Double.NaN;
    }
    if (!teams.stream()
              .allMatch(playerStats -> playerStats.stream().allMatch(stats -> stats.leaderboardRatingJournals().stream()
        .findFirst().map(ratingJournal -> ratingJournal.meanBefore() != null && ratingJournal.deviationBefore() != null)
        .orElse(false)))) {
      return Double.NaN;
    }
    return TrueSkillCalculator.calculateMatchQuality(gameInfo, teams.stream()
        .map(players -> {
          Team team = new Team();
          players.forEach(stats -> {
            stats.leaderboardRatingJournals().stream().findFirst().ifPresent(
                ratingJournal -> {
                  if (ratingJournal.meanBefore() != null && ratingJournal.deviationBefore() != null) {
                    team.addPlayer(new jskills.Player<>(stats.player().getId()),
                                   new Rating(ratingJournal.meanBefore(), ratingJournal.deviationBefore())
                    );
                  }
                }
            );
          });
          return team;
        })
        .collect(Collectors.toList()));
  }
}
