package com.faforever.client.stats;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.api.Leaderboard;
import com.faforever.client.domain.api.LeaderboardRatingJournal;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.mapstruct.LeaderboardMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import com.faforever.commons.api.elide.ElideEntity;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StatisticsServiceTest extends ServiceTest {

  @Mock
  private FafApiAccessor fafApiAccessor;

  @InjectMocks
  private StatisticsService instance;
  private Leaderboard leaderboard;
  @Spy
  private LeaderboardMapper leaderboardMapper = Mappers.getMapper(LeaderboardMapper.class);

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(leaderboardMapper);
    when(fafApiAccessor.getMaxPageSize()).thenReturn(10000);
    leaderboard = Instancio.create(Leaderboard.class);
  }

  @Test
  public void testGetStatisticsForPlayer() throws Exception {
    LeaderboardRatingJournal leaderboardRatingJournal = Instancio.create(LeaderboardRatingJournal.class);
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().username("junit").get();
    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leaderboardRatingJournal));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    StepVerifier.create(instance.getRatingHistory(player, leaderboard)).expectNextCount(1)
                .expectComplete()
                .verify();
    verify(fafApiAccessor).getMany(argThat(
        ElideMatchers.hasFilter(qBuilder().intNum("gamePlayerStats.player.id").eq(player.getId()).and()
                                          .intNum("leaderboard.id")
                                          .eq(leaderboard.id()))
    ));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasPageSize(10000)));
  }
}
