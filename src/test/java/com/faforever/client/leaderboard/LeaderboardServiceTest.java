package com.faforever.client.leaderboard;


import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.LeaderboardBeanBuilder;
import com.faforever.client.builders.LeaderboardEntryBeanBuilder;
import com.faforever.client.builders.LeagueBeanBuilder;
import com.faforever.client.builders.LeagueEntryBeanBuilder;
import com.faforever.client.builders.LeagueSeasonBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.SubdivisionBeanBuilder;
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
import com.faforever.commons.api.dto.LeagueSeasonDivisionSubdivision;
import com.faforever.commons.api.dto.LeagueSeasonScore;
import com.faforever.commons.api.elide.ElideEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;

import java.nio.file.Path;
import java.util.List;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
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
  private PlayerBean player;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(leaderboardMapper);
    player = PlayerBeanBuilder.create().defaultValues().id(1).username("junit").get();
  }

  @Test
  public void testGetLeaderboards() {
    LeaderboardBean leaderboardBean = LeaderboardBeanBuilder.create().defaultValues().get();

    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leaderboardBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getLeaderboards()).expectNext(leaderboardBean).verifyComplete();

    verify(fafApiAccessor).getMany(any());
  }

  @Test
  public void testGetEntriesForPlayer() {
    LeaderboardEntryBean leaderboardEntryBean = LeaderboardEntryBeanBuilder.create().defaultValues().get();
    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leaderboardEntryBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getEntriesForPlayer(player)).expectNext(leaderboardEntryBean).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("player.id").eq(player.getId()))));
  }

  @Test
  public void testGetLeagues() {
    LeagueBean leagueBean = LeagueBeanBuilder.create().defaultValues().get();

    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leagueBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getLeagues()).expectNext(leagueBean).verifyComplete();

    verify(fafApiAccessor).getMany(any());
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().bool("enabled").isTrue())));
  }

  @Test
  public void testGetActiveSeasons() {
    LeagueSeasonBean leagueSeasonBean = LeagueSeasonBeanBuilder.create().defaultValues().get();

    Flux<ElideEntity> resultFlux = Flux.just(
        leaderboardMapper.map(leagueSeasonBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getActiveSeasons()).expectNext(leagueSeasonBean).verifyComplete();
  }

  @Test
  public void testGetLatestSeason() {
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());
    LeagueBean league = LeagueBeanBuilder.create().defaultValues().get();

    StepVerifier.create(instance.getLatestSeason(league)).verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasSort("startDate", false)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.filterPresent()));
  }

  @Test
  public void testGetPlayerNumberInHigherDivisions() {
    SubdivisionBean subdivisionBean1 = SubdivisionBeanBuilder.create().defaultValues().index(1).get();
    SubdivisionBean subdivisionBean2 = SubdivisionBeanBuilder.create().defaultValues().index(2).get();
    SubdivisionBean subdivisionBean3 = SubdivisionBeanBuilder.create().defaultValues().index(3).get();
    LeagueEntryBean leagueEntryBean1 = LeagueEntryBeanBuilder.create().defaultValues().score(8).subdivision(subdivisionBean2).get();
    LeagueEntryBean leagueEntryBean2 = LeagueEntryBeanBuilder.create().defaultValues().get();
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = Mono.zip(
        Mono.just(List.of(
            leaderboardMapper.map(leagueEntryBean1, new CycleAvoidingMappingContext()),
            leaderboardMapper.map(leagueEntryBean2, new CycleAvoidingMappingContext())
        )), Mono.just(2));
    when(fafApiAccessor.getManyWithPageCount(argThat(ElideMatchers.hasDtoClass(LeagueSeasonScore.class)))).thenReturn(resultMono);
    Flux<ElideEntity> resultFlux = Flux.just(
        leaderboardMapper.map(subdivisionBean1, new CycleAvoidingMappingContext()),
        leaderboardMapper.map(subdivisionBean2, new CycleAvoidingMappingContext()),
        leaderboardMapper.map(subdivisionBean3, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(argThat(ElideMatchers.hasDtoClass(LeagueSeasonDivisionSubdivision.class)))).thenReturn(resultFlux);

    StepVerifier.create(instance.getPlayerNumberInHigherDivisions(subdivisionBean2)).expectNext(2).verifyComplete();
  }

  @Test
  public void testGetTotalPlayers() {
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(Mono.zip(Mono.just(List.of()), Mono.just(0)));
    LeagueSeasonBean season = LeagueSeasonBeanBuilder.create().defaultValues().get();

    StepVerifier.create(instance.getTotalPlayers(season)).expectNext(0).verifyComplete();

    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.filterPresent()));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(1)));
  }

  @Test
  public void testGetSizeOfDivision() {
    SubdivisionBean subdivisionBean = SubdivisionBeanBuilder.create().defaultValues().get();
    LeagueEntryBean leagueEntryBean = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean).get();
    Mono<Tuple2<List<ElideEntity>, Integer>> resultMono = Mono.zip(
        Mono.just(List.of(leaderboardMapper.map(leagueEntryBean, new CycleAvoidingMappingContext()))), Mono.just(1));
    when(fafApiAccessor.getManyWithPageCount(any())).thenReturn(resultMono);

    StepVerifier.create(instance.getSizeOfDivision(subdivisionBean)).expectNext(1).verifyComplete();

    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.filterPresent()));
    verify(fafApiAccessor).getManyWithPageCount(argThat(ElideMatchers.hasPageSize(1)));
  }

  @Test
  public void testGetLeagueEntryForPlayer() {
    LeagueEntryBean leagueEntryBean = LeagueEntryBeanBuilder.create().defaultValues().get();
    LeagueSeasonBean season = LeagueSeasonBeanBuilder.create().defaultValues().id(2).get();
    Flux<ElideEntity> resultFlux = Flux.just(
        leaderboardMapper.map(leagueEntryBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getLeagueEntryForPlayer(player, season)).expectNext(leagueEntryBean).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("loginId").eq(player.getId())
        .and().intNum("leagueSeason.id").eq(2))));
  }

  @Test
  public void testGetHighestActiveLeagueEntryForPlayer() {
    SubdivisionBean subdivisionBean1 = SubdivisionBeanBuilder.create().defaultValues().index(2).get();
    SubdivisionBean subdivisionBean2 = SubdivisionBeanBuilder.create().defaultValues().index(3).get();
    LeagueEntryBean leagueEntryBean1 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean1).get();
    LeagueEntryBean leagueEntryBean2 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean2).get();
    LeagueEntryBean leagueEntryBean3 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(null).get();

    Flux<ElideEntity> resultFlux = Flux.just(
        leaderboardMapper.map(leagueEntryBean1, new CycleAvoidingMappingContext()),
        leaderboardMapper.map(leagueEntryBean2, new CycleAvoidingMappingContext()),
        leaderboardMapper.map(leagueEntryBean3, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getHighestActiveLeagueEntryForPlayer(player))
                .expectNext(leagueEntryBean2)
                .verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("loginId").eq(player.getId()))));
  }

  @Test
  public void testGetActiveLeagueEntryForPlayer() {
    LeaderboardBean leaderboard = LeaderboardBeanBuilder.create().defaultValues().id(2).technicalName("ladder").get();
    LeagueSeasonBean season = LeagueSeasonBeanBuilder.create().defaultValues().leaderboard(leaderboard).get();
    LeagueEntryBean leagueEntryBean1 = LeagueEntryBeanBuilder.create().defaultValues().get();
    LeagueEntryBean leagueEntryBean2 = LeagueEntryBeanBuilder.create().defaultValues().leagueSeason(season).get();

    Flux<ElideEntity> resultFlux = Flux.just(
        leaderboardMapper.map(leagueEntryBean1, new CycleAvoidingMappingContext()),
        leaderboardMapper.map(leagueEntryBean2, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getActiveLeagueEntryForPlayer(player, "ladder"))
                .expectNext(leagueEntryBean2)
                .verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("loginId").eq(player.getId())
        .and().string("leagueSeason.leaderboard.technicalName").eq(leaderboard.getTechnicalName()))));
  }

  @Test
  public void testGetActiveLeagueEntriesForPlayer() {
    LeagueEntryBean leagueEntryBean1 = LeagueEntryBeanBuilder.create().defaultValues().get();
    LeagueEntryBean leagueEntryBean2 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(null).get();

    Flux<ElideEntity> resultFlux = Flux.just(
        leaderboardMapper.map(leagueEntryBean1, new CycleAvoidingMappingContext()),
        leaderboardMapper.map(leagueEntryBean2, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getActiveLeagueEntriesForPlayer(player)).expectNext(leagueEntryBean1).verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("loginId").eq(player.getId()))));
  }

  @Test
  public void testGetHighestLeagueEntryForPlayerNoSubdivision() {
    LeagueEntryBean leagueEntryBean = LeagueEntryBeanBuilder.create().defaultValues().subdivision(null).get();

    Flux<ElideEntity> resultFlux = Flux.just(
        leaderboardMapper.map(leagueEntryBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    StepVerifier.create(instance.getHighestActiveLeagueEntryForPlayer(player)).verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("loginId").eq(player.getId()))));
  }

  @Test
  public void testGetLeagueEntries() {
    SubdivisionBean subdivisionBean = SubdivisionBeanBuilder.create().defaultValues().get();
    LeagueEntryBean leagueEntryBean = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean).get();
    Flux<ElideEntity> resultFlux = Flux.just(
        leaderboardMapper.map(leagueEntryBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    when(playerService.getPlayersByIds(anyCollection())).thenReturn(
        Flux.just(PlayerBeanBuilder.create().id(1).username("junit").get()));

    StepVerifier.create(instance.getEntries(subdivisionBean)).expectNext(leagueEntryBean).verifyComplete();
  }

  @Test
  public void testGetLeagueEntriesEmpty() {
    SubdivisionBean subdivisionBean = SubdivisionBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());
    when(playerService.getPlayersByIds(anyCollection())).thenReturn(Flux.empty());
    StepVerifier.create(instance.getEntries(subdivisionBean)).verifyComplete();
  }


  @Test
  public void testGetAllSubdivisions() {
    LeagueSeasonBean season = LeagueSeasonBeanBuilder.create().defaultValues().id(0).get();
    SubdivisionBean subdivisionBean = SubdivisionBeanBuilder.create().defaultValues().get();
    Flux<ElideEntity> resultFlux = Flux.just(
        leaderboardMapper.map(subdivisionBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getAllSubdivisions(season)).expectNext(subdivisionBean).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(
        qBuilder().string("leagueSeasonDivision.leagueSeason.id").eq("0"))));
  }

  @Test
  public void testLoadDivisionImage() {
    SubdivisionBean subdivisionBean = SubdivisionBeanBuilder.create().defaultValues().get();
    instance.loadDivisionImage(subdivisionBean.getImageUrl());
    verify(assetService).loadAndCacheImage(subdivisionBean.getImageUrl(), Path.of("divisions"));
  }
}
