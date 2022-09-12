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
import com.faforever.client.remote.AssetService;
import com.faforever.commons.api.dto.Leaderboard;
import com.faforever.commons.api.dto.LeaderboardEntry;
import com.faforever.commons.api.dto.League;
import com.faforever.commons.api.dto.LeagueSeason;
import com.faforever.commons.api.dto.LeagueSeasonDivisionSubdivision;
import com.faforever.commons.api.dto.LeagueSeasonScore;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;

import java.net.URL;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;


@Lazy
@Service
@RequiredArgsConstructor
public class LeaderboardService {

  private final AssetService assetService;
  private final FafApiAccessor fafApiAccessor;
  private final LeaderboardMapper leaderboardMapper;
  private final PlayerService playerService;

  private static final Comparator<SubdivisionBean> SUBDIVISION_COMPARATOR =
      Comparator.<SubdivisionBean, Integer>comparing(subdivision ->
          subdivision.getDivision().getIndex()).thenComparing(SubdivisionBean::getIndex);

  @Cacheable(value = CacheNames.LEADERBOARD, sync = true)
  public CompletableFuture<List<LeaderboardBean>> getLeaderboards() {
    ElideNavigatorOnCollection<Leaderboard> navigator = ElideNavigator.of(Leaderboard.class).collection();
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

  @Cacheable(value = CacheNames.LEAGUE, sync = true)
  public CompletableFuture<List<LeagueBean>> getLeagues() {
    ElideNavigatorOnCollection<League> navigator = ElideNavigator.of(League.class).collection();
    return fafApiAccessor.getMany(navigator)
        .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
        .collectList()
        .toFuture();
  }

  @Cacheable(value = CacheNames.DIVISIONS, sync = true)
  public CompletableFuture<List<LeagueSeasonBean>> getActiveSeasons() {
    ElideNavigatorOnCollection<LeagueSeason> navigator = ElideNavigator.of(LeagueSeason.class).collection()
        .setFilter(qBuilder()
            .instant("startDate").before(OffsetDateTime.now().toInstant(), false)
            .and()
            .instant("endDate").after(OffsetDateTime.now().toInstant(), false));
    return fafApiAccessor.getMany(navigator)
        .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
        .collectList()
        .toFuture();
  }

  @Cacheable(value = CacheNames.LEAGUE, sync = true)
  public CompletableFuture<LeagueSeasonBean> getLatestSeason(LeagueBean league) {
    ElideNavigatorOnCollection<LeagueSeason> navigator = ElideNavigator.of(LeagueSeason.class).collection()
        .setFilter(qBuilder()
            .intNum("league.id").eq(league.getId())
            .and()
            .instant("startDate").before(OffsetDateTime.now().toInstant(), false))
        .addSortingRule("startDate", false);
    return fafApiAccessor.getMany(navigator)
        .next()
        .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
        .toFuture();
  }

  @Cacheable(value = CacheNames.LEAGUE, sync = true)
  public CompletableFuture<Integer> getPlayerNumberInHigherDivisions(SubdivisionBean subdivision) {
    AtomicInteger rank = new AtomicInteger();
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    return getAllSubdivisions(subdivision.getDivision().getLeagueSeason()).thenCompose(divisions -> {
      divisions.stream()
          .filter(division -> SUBDIVISION_COMPARATOR.compare(division, subdivision) > 0)
          .forEach(division -> {
            CompletableFuture<Void> future = getSizeOfDivision(division).thenAccept(rank::addAndGet);
            futures.add(future);
          });
      return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(aVoid -> rank.get());
    });
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public CompletableFuture<Integer> getTotalPlayers(LeagueSeasonBean leagueSeason) {
    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class).collection()
        .setFilter(qBuilder()
            .intNum("leagueSeason.id").eq(leagueSeason.getId())
            .and()
            .intNum("score").gt(-1))
        .pageSize(1);
    return fafApiAccessor.getManyWithPageCount(navigator)
        .toFuture()
        .thenApply(Tuple2::getT2);
  }

  @Cacheable(value = CacheNames.DIVISIONS, sync = true)
  public CompletableFuture<Integer> getSizeOfDivision(SubdivisionBean subdivision) {
    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class).collection()
        .setFilter(qBuilder().intNum("leagueSeason.id").eq(subdivision.getDivision().getLeagueSeason().getId())
            .and().intNum("leagueSeasonDivisionSubdivision.subdivisionIndex").eq(subdivision.getIndex())
            .and().intNum("leagueSeasonDivisionSubdivision.leagueSeasonDivision.divisionIndex").eq(subdivision.getDivision().getIndex()))
        .pageSize(1);
    return fafApiAccessor.getManyWithPageCount(navigator)
        .toFuture()
        .thenApply(Tuple2::getT2);
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public CompletableFuture<LeagueEntryBean> getLeagueEntryForPlayer(PlayerBean player, LeagueSeasonBean leagueSeason) {
    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class).collection()
        .setFilter(qBuilder()
            .intNum("loginId").eq(player.getId())
            .and()
            .intNum("leagueSeason.id").eq(leagueSeason.getId()));
    return fafApiAccessor.getMany(navigator)
        .next()
        .map(dto -> leaderboardMapper.map(dto, player, new CycleAvoidingMappingContext()))
        .toFuture();
  }

  public CompletableFuture<Optional<LeagueEntryBean>> getHighestActiveLeagueEntryForPlayer(PlayerBean player) {
    return getActiveLeagueEntriesForPlayer(player)
        .thenApply(leagueEntryBeans -> leagueEntryBeans.stream()
            .max((e1, e2) -> SUBDIVISION_COMPARATOR.compare(e1.getSubdivision(), e2.getSubdivision())));
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public CompletableFuture<Optional<LeagueEntryBean>> getActiveLeagueEntryForPlayer(PlayerBean player, String leaderboardName) {
    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class).collection()
        .setFilter(qBuilder()
            .intNum("loginId").eq(player.getId())
            .and()
            .string("leagueSeason.leaderboard.technicalName").eq(leaderboardName)
            .and()
            .instant("leagueSeason.startDate").before(OffsetDateTime.now().toInstant(), false)
            .and()
            .instant("leagueSeason.endDate").after(OffsetDateTime.now().toInstant(), false)
        );
    return fafApiAccessor.getMany(navigator)
        .next()
        .map(dto -> leaderboardMapper.map(dto, player, new CycleAvoidingMappingContext()))
        .filter(leagueEntryBean -> leagueEntryBean.getSubdivision() != null)
        .toFuture()
        .thenApply(Optional::ofNullable);
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public CompletableFuture<List<LeagueEntryBean>> getActiveLeagueEntriesForPlayer(PlayerBean player) {
    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class).collection()
        .setFilter(qBuilder()
            .intNum("loginId").eq(player.getId())
            .and()
            .instant("leagueSeason.startDate").before(OffsetDateTime.now().toInstant(), false)
            .and()
            .instant("leagueSeason.endDate").after(OffsetDateTime.now().toInstant(), false)
        );
    return fafApiAccessor.getMany(navigator)
        .map(dto -> leaderboardMapper.map(dto, player, new CycleAvoidingMappingContext()))
        .filter(leagueEntryBean -> leagueEntryBean.getSubdivision() != null)
        .collectList()
        .toFuture();
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public CompletableFuture<List<LeagueEntryBean>> getEntries(SubdivisionBean subdivision) {
    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class).collection()
        .setFilter(qBuilder().intNum("leagueSeason.id").eq(subdivision.getDivision().getLeagueSeason().getId())
            .and().intNum("leagueSeasonDivisionSubdivision.subdivisionIndex").eq(subdivision.getIndex())
            .and().intNum("leagueSeasonDivisionSubdivision.leagueSeasonDivision.divisionIndex").eq(subdivision.getDivision().getIndex()))
        .addSortingRule("score", false);
    return mapLeagueEntryDtoToBean(fafApiAccessor.getMany(navigator)
        .collectList()
        .toFuture()
    );
  }

  private CompletableFuture<List<LeagueEntryBean>> mapLeagueEntryDtoToBean(CompletableFuture<List<LeagueSeasonScore>> future) {
    return future.thenCompose(leagueSeasonScores -> {
      if (leagueSeasonScores.isEmpty()) {
        return CompletableFuture.completedFuture(Collections.emptyList());
      }
      List<Integer> playerIds = leagueSeasonScores
          .stream()
          .map(LeagueSeasonScore::getLoginId)
          .collect(Collectors.toList());
      return playerService.getPlayersByIds(playerIds).thenApply(playerBeans ->
          leagueSeasonScores
              .stream()
              .flatMap(leagueSeasonScore ->
                  playerBeans
                      .stream()
                      .filter(playerBean -> playerBean.getId().equals(leagueSeasonScore.getLoginId()))
                      .map(playerBean -> leaderboardMapper.map(leagueSeasonScore, playerBean, new CycleAvoidingMappingContext())))
              .collect(Collectors.toList()));
    });
  }

  @Cacheable(value = CacheNames.DIVISIONS, sync = true)
  public CompletableFuture<List<SubdivisionBean>> getAllSubdivisions(LeagueSeasonBean leagueSeason) {
    ElideNavigatorOnCollection<LeagueSeasonDivisionSubdivision> navigator = ElideNavigator.of(LeagueSeasonDivisionSubdivision.class).collection()
        .setFilter(qBuilder().string("leagueSeasonDivision.leagueSeason.id").eq(String.valueOf(leagueSeason.getId())));
    return fafApiAccessor.getMany(navigator)
        .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
        .collectList()
        .toFuture();
  }

  @Cacheable(value = CacheNames.DIVISIONS, sync = true)
  public Image loadDivisionImage(URL url) {
    return assetService.loadAndCacheImage(url, Path.of("divisions"), null);
  }
}
