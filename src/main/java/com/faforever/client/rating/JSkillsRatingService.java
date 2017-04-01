package com.faforever.client.rating;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.TrueSkill;
import com.faforever.client.replay.Replay;
import com.faforever.client.replay.Replay.PlayerStats;
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
  private GameInfo gameInfo;

  public JSkillsRatingService(ClientProperties clientProperties) {
    TrueSkill trueSkill = clientProperties.getTrueSkill();
    gameInfo = new GameInfo(trueSkill.getInitialMean(), trueSkill.getInitialStandardDeviation(), trueSkill.getBeta(),
        trueSkill.getDynamicFactor(), trueSkill.getDrawProbability());
  }

  @Override
  public double calculateQuality(Replay replay) {
    Collection<List<PlayerStats>> teams = replay.getTeamPlayerStats().values();
    if (teams.size() < 2) {
      return Double.NaN;
    }
    return TrueSkillCalculator.calculateMatchQuality(gameInfo, teams.stream()
        .map(players -> {
          Team team = new Team();
          players.forEach(stats -> team.addPlayer(
              new jskills.Player<>(stats.getPlayerId()), new Rating(stats.getMean(), stats.getDeviation())
          ));
          return team;
        })
        .collect(Collectors.toList()));
  }
}
