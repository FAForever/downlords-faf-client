package com.faforever.client.rating;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.TrueSkill;
import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.ReplayBean;
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
  public double calculateQuality(ReplayBean replay) {
    Collection<List<GamePlayerStatsBean>> teams = replay.getTeamPlayerStats().values();
    if (teams.size() != 2) {
      return Double.NaN;
    }
    if (!teams.stream().allMatch(playerStats -> playerStats.stream().allMatch(stats -> stats.getLeaderboardRatingJournals().stream()
        .findFirst()
        .map(ratingJournal -> ratingJournal.getMeanBefore() != null && ratingJournal.getDeviationBefore() != null)
        .orElse(false)))) {
      return Double.NaN;
    }
    return TrueSkillCalculator.calculateMatchQuality(gameInfo, teams.stream()
        .map(players -> {
          Team team = new Team();
          players.forEach(stats -> {
            stats.getLeaderboardRatingJournals().stream().findFirst().ifPresent(
                ratingJournal -> {
                  if (ratingJournal.getMeanBefore() != null && ratingJournal.getDeviationBefore() != null) {
                    team.addPlayer(
                        new jskills.Player<>(stats.getPlayer().getId()), new Rating(ratingJournal.getMeanBefore(), ratingJournal.getDeviationBefore())
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
