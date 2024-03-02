package com.faforever.client.leaderboard;


import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.DivisionBean;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardEntryBean;
import com.faforever.client.domain.LeagueBean;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.LeagueSeasonBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.LeaderboardMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
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

import java.nio.file.Path;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LeaderboardServiceTest extends ServiceTest {

  @Mock
  private AssetService assetService;
  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private PlayerService playerService;

  @InjectMocks
  private LeaderboardService instance;

  @Spy
  private final LeaderboardMapper leaderboardMapper = Mappers.getMapper(LeaderboardMapper.class);
  private final PlayerBean player = PlayerBeanBuilder.create().defaultValues().id(1).username("junit").get();

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(leaderboardMapper);
  }

  @Test
  public void testGetLeaderboards() {
    LeaderboardBean leaderboardBean = Instancio.create(LeaderboardBean.class);

    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leaderboardBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getLeaderboards()).expectNext(leaderboardBean).verifyComplete();

    verify(fafApiAccessor).getMany(any());
  }

  @Test
  public void testGetEntriesForPlayer() {
    LeaderboardEntryBean leaderboardEntryBean = Instancio.create(LeaderboardEntryBean.class);
    Flux<ElideEntity> resultFlux = Flux.just(
        leaderboardMapper.map(leaderboardEntryBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getEntriesForPlayer(player)).expectNext(leaderboardEntryBean).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("player.id").eq(player.getId()))));
  }

  @Test
  public void testGetLeagues() {
    LeagueBean leagueBean = Instancio.create(LeagueBean.class);

    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leagueBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getLeagues()).expectNext(leagueBean).verifyComplete();

    verify(fafApiAccessor).getMany(any());
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().bool("enabled").isTrue())));
  }

  @Test
  public void testGetActiveSeasons() {
    LeagueSeasonBean leagueSeasonBean = Instancio.create(LeagueSeasonBean.class);

    Flux<ElideEntity> resultFlux = Flux.just(
        leaderboardMapper.map(leagueSeasonBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getActiveSeasons()).expectNext(leagueSeasonBean).verifyComplete();
  }

  @Test
  public void testGetLatestSeason() {
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());
    LeagueBean league = Instancio.create(LeagueBean.class);

    StepVerifier.create(instance.getLatestSeason(league)).verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasSort("startDate", false)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.filterPresent()));
  }

  @Test
  public void testGetLeagueEntryForPlayer() {
    LeagueEntryBean leagueEntryBean = Instancio.of(LeagueEntryBean.class)
                                               .set(field(LeagueEntryBean::player), player)
                                               .set(field(LeagueEntryBean::rank), null)
                                               .create();
    LeagueSeasonBean season = Instancio.of(LeagueSeasonBean.class).set(field(LeagueSeasonBean::id), 2).create();
    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leagueEntryBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getLeagueEntryForPlayer(player, season)).expectNext(leagueEntryBean).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(
        qBuilder().intNum("loginId").eq(player.getId()).and().intNum("leagueSeason.id").eq(2))));
  }

  @Test
  public void testGetHighestActiveLeagueEntryForPlayer() {
    DivisionBean divisionBean = Instancio.create(DivisionBean.class);
    SubdivisionBean subdivisionBean1 = Instancio.of(SubdivisionBean.class)
                                                .set(field(SubdivisionBean::index), 2)
                                                .set(field(SubdivisionBean::division), divisionBean)
                                                .create();
    SubdivisionBean subdivisionBean2 = Instancio.of(SubdivisionBean.class)
                                                .set(field(SubdivisionBean::index), 3)
                                                .set(field(SubdivisionBean::division), divisionBean)
                                                .create();
    LeagueEntryBean leagueEntryBean1 = Instancio.of(LeagueEntryBean.class)
                                                .set(field(LeagueEntryBean::player), player)
                                                .set(field(LeagueEntryBean::subdivision), subdivisionBean1)
                                                .ignore(field(LeagueEntryBean::rank))
                                                .create();
    LeagueEntryBean leagueEntryBean2 = Instancio.of(LeagueEntryBean.class)
                                                .set(field(LeagueEntryBean::player), player)
                                                .set(field(LeagueEntryBean::subdivision), subdivisionBean2)
                                                .ignore(field(LeagueEntryBean::rank))
                                                .create();
    LeagueEntryBean leagueEntryBean3 = Instancio.of(LeagueEntryBean.class)
                                                .set(field(LeagueEntryBean::player), player)
                                                .ignore(field(LeagueEntryBean::subdivision))
                                                .ignore(field(LeagueEntryBean::rank))
                                                .create();

    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leagueEntryBean1, new CycleAvoidingMappingContext()),
                                             leaderboardMapper.map(leagueEntryBean2, new CycleAvoidingMappingContext()),
                                             leaderboardMapper.map(leagueEntryBean3,
                                                                   new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getHighestActiveLeagueEntryForPlayer(player))
                .expectNext(leagueEntryBean2)
                .verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("loginId").eq(player.getId()))));
  }

  @Test
  public void testGetActiveLeagueEntryForPlayer() {
    LeaderboardBean leaderboard = Instancio.of(LeaderboardBean.class)
                                           .set(field(LeaderboardBean::technicalName), "ladder")
                                           .set(field(LeaderboardBean::id), 2)
                                           .create();
    LeagueEntryBean leagueEntryBean1 = Instancio.of(LeagueEntryBean.class)
                                                .set(field(LeagueEntryBean::rank), null)
                                                .set(field(LeagueEntryBean::subdivision), null)
                                                .set(field(LeagueEntryBean::player), player)
                                                .create();
    LeagueEntryBean leagueEntryBean2 = Instancio.of(LeagueEntryBean.class)
                                                .set(field(LeagueEntryBean::rank), null)
                                                .set(field(LeagueEntryBean::player), player)
                                                .create();

    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leagueEntryBean1, new CycleAvoidingMappingContext()),
                                             leaderboardMapper.map(leagueEntryBean2,
                                                                   new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getActiveLeagueEntryForPlayer(player, "ladder"))
                .expectNext(leagueEntryBean2)
                .verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("loginId").eq(player.getId())
                                                                             .and()
                                                                             .string(
                                                                                 "leagueSeason.leagueLeaderboard.technicalName")
                                                                             .eq(leaderboard.technicalName()))));
  }

  @Test
  public void testGetHighestLeagueEntryForPlayerNoSubdivision() {
    LeagueEntryBean leagueEntryBean = Instancio.of(LeagueEntryBean.class)
                                               .set(field(LeagueEntryBean::subdivision), null)
                                               .create();

    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leagueEntryBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    StepVerifier.create(instance.getHighestActiveLeagueEntryForPlayer(player)).verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("loginId").eq(player.getId()))));
  }

  @Test
  public void testGetLeagueEntries() {
    LeagueSeasonBean leagueSeasonBean = Instancio.create(LeagueSeasonBean.class);
    LeagueEntryBean leagueEntryBean = Instancio.of(LeagueEntryBean.class)
                                               .set(field(LeagueEntryBean::rank), 0L)
                                               .set(field(LeagueEntryBean::player), player)
                                               .create();
    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leagueEntryBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    when(playerService.getPlayersByIds(anyCollection())).thenReturn(
        Flux.just(PlayerBeanBuilder.create().id(1).username("junit").get()));

    StepVerifier.create(instance.getActiveEntries(leagueSeasonBean)).expectNext(leagueEntryBean).verifyComplete();
  }

  @Test
  public void testGetLeagueEntriesEmpty() {
    LeagueSeasonBean leagueSeasonBean = Instancio.create(LeagueSeasonBean.class);
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());
    when(playerService.getPlayersByIds(anyCollection())).thenReturn(Flux.empty());
    StepVerifier.create(instance.getActiveEntries(leagueSeasonBean)).verifyComplete();
  }


  @Test
  public void testGetAllSubdivisions() {
    LeagueSeasonBean season = Instancio.create(LeagueSeasonBean.class);
    SubdivisionBean subdivisionBean = Instancio.create(SubdivisionBean.class);
    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(subdivisionBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getAllSubdivisions(season)).expectNext(subdivisionBean).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(
        ElideMatchers.hasFilter(qBuilder().string("leagueSeasonDivision.leagueSeason.id").eq(season.id().toString()))));
  }

  @Test
  public void testLoadDivisionImage() {
    SubdivisionBean subdivisionBean = Instancio.create(SubdivisionBean.class);
    instance.loadDivisionImage(subdivisionBean.imageUrl());
    verify(assetService).loadAndCacheImage(subdivisionBean.imageUrl(), Path.of("divisions"));
  }
}
