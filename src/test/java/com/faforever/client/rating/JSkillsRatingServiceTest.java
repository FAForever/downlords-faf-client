package com.faforever.client.rating;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.TrueSkill;
import com.faforever.client.domain.api.GamePlayerStats;
import com.faforever.client.domain.api.LeaderboardRatingJournal;
import com.faforever.client.domain.api.Replay;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notANumber;

public class JSkillsRatingServiceTest extends ServiceTest {

  private JSkillsRatingService instance;

  @BeforeEach
  public void setUp() {
    TrueSkill trueSkill = new TrueSkill();
    trueSkill.setInitialMean(1500);
    trueSkill.setInitialStandardDeviation(500);
    trueSkill.setBeta(240);
    trueSkill.setDynamicFactor(10);
    trueSkill.setDrawProbability(0.1f);
    ClientProperties clientProperties = new ClientProperties();
    clientProperties.setTrueSkill(trueSkill);

    instance = new JSkillsRatingService(clientProperties);
  }

  @Test
  public void calculateQualityOfTwoVersusTwo() {
    Replay replay = replay(Map.of(
        "1", List.of(stats(1, 1500d, 100d), stats(2, 1200d, 150d)),
        "2", List.of(stats(3, 1400d, 80d), stats(4, 1300d, 200d))
    ));

    assertThat(instance.calculateQuality(replay), closeTo(EXPECTED_QUALITY, 1e-12));
  }

  @Test
  public void calculateQualityOfThreeTeamsIsNaN() {
    Replay replay = replay(Map.of(
        "1", List.of(stats(1, 1500d, 100d)),
        "2", List.of(stats(2, 1500d, 100d)),
        "3", List.of(stats(3, 1500d, 100d))
    ));

    assertThat(instance.calculateQuality(replay), is(notANumber()));
  }

  @Test
  public void calculateQualityWithoutRatingIsNaN() {
    Replay replay = replay(Map.of(
        "1", List.of(stats(1, 1500d, 100d)),
        "2", List.of(stats(2, null, null))
    ));

    assertThat(instance.calculateQuality(replay), is(notANumber()));
  }

  private static final double EXPECTED_QUALITY = 0.86308044610572;

  private static Replay replay(Map<String, List<GamePlayerStats>> teamPlayerStats) {
    return new Replay(1, "test", true, null, teamPlayerStats, null, null, null, null, null, null, null, null, null,
                      null, null, false);
  }

  private static GamePlayerStats stats(int playerId, Double meanBefore, Double deviationBefore) {
    LeaderboardRatingJournal journal = new LeaderboardRatingJournal(playerId, null, null, meanBefore, deviationBefore,
                                                                    null, null);
    return new GamePlayerStats(false, null, (byte) 0, (byte) 0, (byte) 0, (byte) 0, null, null,
                               PlayerInfoBuilder.create().defaultValues().id(playerId).get(), List.of(journal));
  }
}
