package com.faforever.client.leaderboard;


import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.LeaderboardBeanBuilder;
import com.faforever.client.builders.LeaderboardEntryBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardEntryBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.LeaderboardMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import reactor.core.publisher.Flux;

import java.util.List;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LeaderboardServiceTest extends ServiceTest {

  @Mock
  private FafApiAccessor fafApiAccessor;

  private LeaderboardService instance;

  private LeaderboardBean leaderboard;
  private final LeaderboardMapper leaderboardMapper = Mappers.getMapper(LeaderboardMapper.class);
  private PlayerBean player;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(leaderboardMapper);
    player = PlayerBeanBuilder.create().defaultValues().username("junit").get();
    leaderboard = LeaderboardBeanBuilder.create().defaultValues().get();

    instance = new LeaderboardService(fafApiAccessor, leaderboardMapper);
  }

  @Test
  public void testGetLeaderboards() {
    LeaderboardBean leaderboardBean = LeaderboardBeanBuilder.create().defaultValues().get();

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(leaderboardMapper.map(leaderboardBean, new CycleAvoidingMappingContext())));

    List<LeaderboardBean> results = instance.getLeaderboards().toCompletableFuture().join();

    verify(fafApiAccessor).getMany(any());
    assertThat(results, hasSize(1));
    assertThat(results.get(0), is(leaderboardBean));
  }

  @Test
  public void testGetLeaderboardEntries() {
    LeaderboardEntryBean leaderboardEntryBean = LeaderboardEntryBeanBuilder.create().defaultValues().get();

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(leaderboardMapper.map(leaderboardEntryBean, new CycleAvoidingMappingContext())));

    List<LeaderboardEntryBean> results = instance.getEntries(leaderboard).toCompletableFuture().join();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasSort("rating", false)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.filterPresent()));
    assertThat(results, hasSize(1));
    assertThat(results.get(0), is(leaderboardEntryBean));
  }

  @Test
  public void testGetEntriesForPlayer() {
    LeaderboardEntryBean leaderboardEntryBean = LeaderboardEntryBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(leaderboardMapper.map(leaderboardEntryBean, new CycleAvoidingMappingContext())));

    List<LeaderboardEntryBean> result = instance.getEntriesForPlayer(player).toCompletableFuture().join();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("player.id").eq(player.getId()))));
    Assertions.assertEquals(List.of(leaderboardEntryBean), result);
  }
}
