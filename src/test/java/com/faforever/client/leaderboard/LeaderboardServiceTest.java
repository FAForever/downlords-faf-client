package com.faforever.client.leaderboard;


import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.api.Division;
import com.faforever.client.domain.api.Leaderboard;
import com.faforever.client.domain.api.LeaderboardEntry;
import com.faforever.client.domain.api.League;
import com.faforever.client.domain.api.LeagueEntry;
import com.faforever.client.domain.api.LeagueSeason;
import com.faforever.client.domain.api.Subdivision;
import com.faforever.client.domain.server.PlayerInfo;
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
  private final PlayerInfo player = PlayerInfoBuilder.create().defaultValues().id(1).username("junit").get();

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(leaderboardMapper);
  }

  @Test
  public void testGetLeaderboards() {
    Leaderboard leaderboard = Instancio.create(Leaderboard.class);

    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leaderboard, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getLeaderboards()).expectNext(leaderboard).verifyComplete();

    verify(fafApiAccessor).getMany(any());
  }

  @Test
  public void testGetEntriesForPlayer() {
    LeaderboardEntry leaderboardEntry = Instancio.create(LeaderboardEntry.class);
    Flux<ElideEntity> resultFlux = Flux.just(
        leaderboardMapper.map(leaderboardEntry, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getEntriesForPlayer(player)).expectNext(leaderboardEntry).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("player.id").eq(player.getId()))));
  }

  @Test
  public void testGetLeagues() {
    League league = Instancio.create(League.class);

    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(league, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getLeagues()).expectNext(league).verifyComplete();

    verify(fafApiAccessor).getMany(any());
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().bool("enabled").isTrue())));
  }

  @Test
  public void testGetActiveSeasons() {
    LeagueSeason leagueSeason = Instancio.create(LeagueSeason.class);

    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leagueSeason, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getActiveSeasons()).expectNext(leagueSeason).verifyComplete();
  }

  @Test
  public void testGetLatestSeason() {
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());
    League league = Instancio.create(League.class);

    StepVerifier.create(instance.getLatestSeason(league)).verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasSort("startDate", false)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.filterPresent()));
  }

  @Test
  public void testGetLeagueEntryForPlayer() {
    LeagueEntry leagueEntry = Instancio.of(LeagueEntry.class)
                                       .set(field(LeagueEntry::player), player)
                                       .set(field(LeagueEntry::rank), null)
                                       .create();
    LeagueSeason season = Instancio.of(LeagueSeason.class).set(field(LeagueSeason::id), 2).create();
    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leagueEntry, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getLeagueEntryForPlayer(player, season)).expectNext(leagueEntry).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(
        qBuilder().intNum("loginId").eq(player.getId()).and().intNum("leagueSeason.id").eq(2))));
  }

  @Test
  public void testGetHighestActiveLeagueEntryForPlayer() {
    Division division = Instancio.create(Division.class);
    Subdivision subdivision1 = Instancio.of(Subdivision.class)
                                        .set(field(Subdivision::index), 2)
                                        .set(field(Subdivision::division), division)
                                        .create();
    Subdivision subdivision2 = Instancio.of(Subdivision.class)
                                        .set(field(Subdivision::index), 3)
                                        .set(field(Subdivision::division), division)
                                        .create();
    LeagueEntry leagueEntry1 = Instancio.of(LeagueEntry.class)
                                        .set(field(LeagueEntry::player), player)
                                        .set(field(LeagueEntry::subdivision), subdivision1)
                                        .ignore(field(LeagueEntry::rank))
                                        .create();
    LeagueEntry leagueEntry2 = Instancio.of(LeagueEntry.class)
                                        .set(field(LeagueEntry::player), player)
                                        .set(field(LeagueEntry::subdivision), subdivision2)
                                        .ignore(field(LeagueEntry::rank))
                                        .create();
    LeagueEntry leagueEntry3 = Instancio.of(LeagueEntry.class)
                                        .set(field(LeagueEntry::player), player)
                                        .ignore(field(LeagueEntry::subdivision))
                                        .ignore(field(LeagueEntry::rank))
                                        .create();

    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leagueEntry1, new CycleAvoidingMappingContext()),
                                             leaderboardMapper.map(leagueEntry2, new CycleAvoidingMappingContext()),
                                             leaderboardMapper.map(leagueEntry3,
                                                                   new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getHighestActiveLeagueEntryForPlayer(player)).expectNext(leagueEntry2)
                .verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("loginId").eq(player.getId()))));
  }

  @Test
  public void testGetActiveLeagueEntryForPlayer() {
    Leaderboard leaderboard = Instancio.of(Leaderboard.class)
                                       .set(field(Leaderboard::technicalName), "ladder")
                                       .set(field(Leaderboard::id), 2)
                                       .create();
    LeagueEntry leagueEntry1 = Instancio.of(LeagueEntry.class)
                                        .set(field(LeagueEntry::rank), null)
                                        .set(field(LeagueEntry::subdivision), null)
                                        .set(field(LeagueEntry::player), player)
                                        .create();
    LeagueEntry leagueEntry2 = Instancio.of(LeagueEntry.class)
                                        .set(field(LeagueEntry::rank), null)
                                        .set(field(LeagueEntry::player), player)
                                        .create();

    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leagueEntry1, new CycleAvoidingMappingContext()),
                                             leaderboardMapper.map(leagueEntry2,
                                                                   new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getActiveLeagueEntryForPlayer(player, "ladder")).expectNext(leagueEntry2)
                .verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("loginId").eq(player.getId())
                                                                             .and()
                                                                             .string(
                                                                                 "leagueSeason.leaderboard.technicalName")
                                                                             .eq(leaderboard.technicalName()))));
  }

  @Test
  public void testGetHighestLeagueEntryForPlayerNoSubdivision() {
    LeagueEntry leagueEntry = Instancio.of(LeagueEntry.class).set(field(LeagueEntry::subdivision), null).create();

    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leagueEntry, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    StepVerifier.create(instance.getHighestActiveLeagueEntryForPlayer(player)).verifyComplete();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("loginId").eq(player.getId()))));
  }

  @Test
  public void testGetLeagueEntries() {
    LeagueSeason leagueSeason = Instancio.create(LeagueSeason.class);
    LeagueEntry leagueEntry = Instancio.of(LeagueEntry.class)
                                       .set(field(LeagueEntry::rank), 0L)
                                       .set(field(LeagueEntry::player), player)
                                       .create();
    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(leagueEntry, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);
    when(playerService.getPlayersByIds(anyCollection())).thenReturn(
        Flux.just(PlayerInfoBuilder.create().id(1).username("junit").get()));

    StepVerifier.create(instance.getActiveEntries(leagueSeason)).expectNext(leagueEntry).verifyComplete();
  }

  @Test
  public void testGetLeagueEntriesEmpty() {
    LeagueSeason leagueSeason = Instancio.create(LeagueSeason.class);
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());
    when(playerService.getPlayersByIds(anyCollection())).thenReturn(Flux.empty());
    StepVerifier.create(instance.getActiveEntries(leagueSeason)).verifyComplete();
  }


  @Test
  public void testGetAllSubdivisions() {
    LeagueSeason season = Instancio.create(LeagueSeason.class);
    Subdivision subdivision = Instancio.create(Subdivision.class);
    Flux<ElideEntity> resultFlux = Flux.just(leaderboardMapper.map(subdivision, new CycleAvoidingMappingContext()));
    when(fafApiAccessor.getMany(any())).thenReturn(resultFlux);

    StepVerifier.create(instance.getAllSubdivisions(season)).expectNext(subdivision).verifyComplete();
    verify(fafApiAccessor).getMany(argThat(
        ElideMatchers.hasFilter(qBuilder().string("leagueSeasonDivision.leagueSeason.id").eq(season.id().toString()))));
  }

  @Test
  public void testLoadDivisionImage() {
    Subdivision subdivision = Instancio.create(Subdivision.class);
    instance.loadDivisionImage(subdivision.imageUrl());
    verify(assetService).loadAndCacheImage(subdivision.imageUrl(), Path.of("divisions"));
  }
}
