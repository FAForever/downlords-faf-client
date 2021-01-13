package com.faforever.client.leaderboard;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardEntryBean;
import com.faforever.client.domain.LeagueBean;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.LeagueSeasonBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.LeaderboardMapper;
import com.faforever.client.player.PlayerService;
import com.faforever.commons.api.dto.Leaderboard;
import com.faforever.commons.api.dto.LeaderboardEntry;
import com.faforever.commons.api.dto.League;
import com.faforever.commons.api.dto.LeagueSeason;
import com.faforever.commons.api.dto.LeagueSeasonDivisionSubdivision;
import com.faforever.commons.api.dto.LeagueSeasonScore;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;


@Lazy
@Service
@RequiredArgsConstructor
public class LeaderboardService {

  private final FafApiAccessor fafApiAccessor;
  private final LeaderboardMapper leaderboardMapper;
  private final PlayerService playerService;

  @Cacheable(value = CacheNames.LEADERBOARD, sync = true)
  public CompletableFuture<List<LeaderboardBean>> getLeaderboards() {
    ElideNavigatorOnCollection<Leaderboard> navigator = ElideNavigator.of(Leaderboard.class).collection();
    return fafApiAccessor.getMany(navigator)
        .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
        .collectList()
        .toFuture();
  }

  @Cacheable(value = CacheNames.LEADERBOARD, sync = true)
  public CompletableFuture<List<LeaderboardEntryBean>> getEntries(LeaderboardBean leaderboard) {
    ElideNavigatorOnCollection<LeaderboardEntry> navigator = ElideNavigator.of(LeaderboardEntry.class).collection()
        .setFilter(qBuilder().string("leaderboard.technicalName").eq(leaderboard.getTechnicalName())
            .and().instant("updateTime").after(OffsetDateTime.now().minusMonths(1).toInstant(), false))
        .addSortingRule("rating", false);
    return fafApiAccessor.getMany(navigator)
        .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
        .collectList()
        .toFuture();
  }

  @Cacheable(value = CacheNames.LEADERBOARD, sync = true)
  public CompletableFuture<List<LeaderboardEntryBean>> getEntriesForPlayer(PlayerBean player) {
    ElideNavigatorOnCollection<LeaderboardEntry> navigator = ElideNavigator.of(LeaderboardEntry.class).collection()
        .setFilter(qBuilder().intNum("player.id").eq(player.getId()));
    return fafApiAccessor.getMany(navigator)
        .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
        .collectList()
        .toFuture();
  }

  public CompletableFuture<List<LeagueBean>> getLeagues() {
    ElideNavigatorOnCollection<League> navigator = ElideNavigator.of(League.class).collection();
    return fafApiAccessor.getMany(navigator)
        .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
        .collectList()
        .toFuture();
  }

  public CompletableFuture<LeagueSeasonBean> getLatestSeason(int leagueId) {
    ElideNavigatorOnCollection<LeagueSeason> navigator = ElideNavigator.of(LeagueSeason.class).collection()
        .setFilter(qBuilder().intNum("league.id").eq(leagueId))
        .addSortingRule("endDate", true);
    return fafApiAccessor.getMany(navigator)
        .next()
        .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
        .toFuture();
  }

  public CompletableFuture<Integer> getAccumulatedRank(LeagueEntryBean entry) {
    if (entry.getSubdivision() == null) {
      Throwable notRanked = new Throwable("Player is not ranked");
      return CompletableFuture.failedFuture(notRanked);
    }
    AtomicInteger rank = new AtomicInteger();
    getAllSubdivisions(entry.getLeagueSeasonId()).thenAccept(divisions -> {
      divisions.stream()
          .filter(division -> division.getDivision().getIndex() >= entry.getSubdivision().getDivision().getIndex())
          .filter(division -> !(division.getDivision().getIndex() == entry.getSubdivision().getDivision().getIndex() && division.getIndex() <= entry.getSubdivision().getIndex()))
          .forEach(division -> getSizeOfDivision(division).thenApply(rank::addAndGet));
      getEntries(entry.getSubdivision()).thenAccept(leagueEntryBeans ->
          rank.addAndGet(leagueEntryBeans.indexOf(entry) + 1));
    });
    return CompletableFuture.completedFuture(rank.get());
  }

  public CompletableFuture<Integer> getTotalPlayers(int leagueSeasonId) {
    AtomicInteger rank = new AtomicInteger();
    getAllSubdivisions(leagueSeasonId).thenAccept(divisions ->
        divisions.forEach(division -> getSizeOfDivision(division).thenApply(rank::addAndGet)));
    return CompletableFuture.completedFuture(rank.get());
  }

  public CompletableFuture<Integer> getSizeOfDivision(SubdivisionBean subdivision) {
    return getEntries(subdivision).thenApply(List::size);
  }

  public CompletableFuture<LeagueEntryBean> getLeagueEntryForPlayer(PlayerBean player, int leagueSeasonId) {
    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class).collection()
        .setFilter(qBuilder()
            .intNum("loginId").eq(player.getId())
            .and()
            .intNum("leagueSeason.id").eq(leagueSeasonId));
    return fafApiAccessor.getMany(navigator)
        .next()
        .map(dto -> leaderboardMapper.map(dto, player.getUsername(), new CycleAvoidingMappingContext()))
        .toFuture();
  }

  public CompletableFuture<List<LeagueEntryBean>> getEntries(SubdivisionBean subdivision) {
    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class).collection()
        .setFilter(qBuilder().intNum("leagueSeason.id").eq(subdivision.getLeagueSeasonId())
            .and().intNum("leagueSeasonDivisionSubdivision.id").eq(subdivision.getIndex()))
        .addSortingRule("score", false);
    return fafApiAccessor.getMany(navigator)
        .collectList()
        .toFuture()
        .thenCompose(leagueSeasonScores -> {
          if (leagueSeasonScores.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
          }
          List<Integer> playerIds = leagueSeasonScores
              .parallelStream()
              .map(LeagueSeasonScore::getLoginId)
              .collect(Collectors.toList());
          return playerService.getPlayersByIds(playerIds).thenApply(playerBeans -> playerBeans
              .parallelStream()
              .flatMap(playerBean -> leagueSeasonScores
                  .stream()
                  .filter(leagueSeasonScore -> leagueSeasonScore.getLoginId().equals(playerBean.getId()))
                  .map(leagueSeasonScore -> leaderboardMapper.map(leagueSeasonScore, playerBean.getUsername(), new CycleAvoidingMappingContext()))
              )
              .collect(Collectors.toList()));
        });
  }

  @Cacheable(value = CacheNames.DIVISIONS, sync = true)
  public CompletableFuture<List<SubdivisionBean>> getAllSubdivisions(int leagueSeasonId) {
    ElideNavigatorOnCollection<LeagueSeasonDivisionSubdivision> navigator = ElideNavigator.of(LeagueSeasonDivisionSubdivision.class).collection()
        .setFilter(qBuilder().string("leagueSeasonDivision.leagueSeason.id").eq(String.valueOf(leagueSeasonId)));
    return fafApiAccessor.getMany(navigator)
        .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
        .collectList()
        .toFuture();
  }
}
