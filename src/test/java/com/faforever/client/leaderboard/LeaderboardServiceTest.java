package com.faforever.client.leaderboard;


import com.faforever.client.api.FafApiAccessor;
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
  private PlayerBean player;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(leaderboardMapper);
    player = PlayerBeanBuilder.create().defaultValues().id(1).username("junit").get();
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
    LeaderboardBean leaderboard = Instancio.of(LeaderboardBean.class)
                                           .set(field(LeaderboardBean::technicalName), "ladder")
                                           .set(field(LeaderboardBean::id), 2)
                                           .create();
    LeagueEntryBean leagueEntryBean1 = LeagueEntryBeanBuilder.create().defaultValues().get();
    LeagueEntryBean leagueEntryBean2 = LeagueEntryBeanBuilder.create().defaultValues().get();

    Flux<ElideEntity> resultFlux = Flux.just(
        leaderboardMapper.map(leagueEntryBean1, new CycleAvoidingMappingContext()),
        leaderboardMapper.map(leagueEntryBean2, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getActiveLeagueEntryForPlayer(player, "ladder"))
                .expectNext(leagueEntryBean2)
                .verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("loginId").eq(player.getId())
                                                                             .and()
                                                                             .string(
                                                                                 "leagueSeason.leaderboard.technicalName")
                                                                             .eq(leaderboard.technicalName()))));
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
    LeagueSeasonBean leagueSeasonBean = LeagueSeasonBeanBuilder.create().defaultValues().get();
    LeagueEntryBean leagueEntryBean = LeagueEntryBeanBuilder.create().defaultValues().get();
    Flux<ElideEntity> resultFlux = Flux.just(
        leaderboardMapper.map(leagueEntryBean, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    when(playerService.getPlayersByIds(anyCollection())).thenReturn(
        Flux.just(PlayerBeanBuilder.create().id(1).username("junit").get()));

    StepVerifier.create(instance.getActiveEntries(leagueSeasonBean)).expectNext(leagueEntryBean).verifyComplete();
  }

  @Test
  public void testGetLeagueEntriesEmpty() {
    LeagueSeasonBean leagueSeasonBean = LeagueSeasonBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());
    when(playerService.getPlayersByIds(anyCollection())).thenReturn(Flux.empty());
    StepVerifier.create(instance.getActiveEntries(leagueSeasonBean)).verifyComplete();
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
