package com.faforever.client.leaderboard;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
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
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.net.URL;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

  @Cacheable(value = CacheNames.LEADERBOARD, sync = true)
  public Flux<LeaderboardBean> getLeaderboards() {
    ElideNavigatorOnCollection<Leaderboard> navigator = ElideNavigator.of(Leaderboard.class).collection();
    return fafApiAccessor.getMany(navigator)
                         .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
                         .cache();
  }

  @Cacheable(value = CacheNames.LEADERBOARD, sync = true)
  public Flux<LeaderboardEntryBean> getEntriesForPlayer(PlayerBean player) {
    ElideNavigatorOnCollection<LeaderboardEntry> navigator = ElideNavigator.of(LeaderboardEntry.class)
                                                                           .collection()
                                                                           .setFilter(qBuilder().intNum("player.id")
                                                                                                .eq(player.getId()));
    return fafApiAccessor.getMany(navigator)
                         .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
                         .cache();
  }

  @Cacheable(value = CacheNames.LEAGUE, sync = true)
  public Flux<LeagueBean> getLeagues() {
    ElideNavigatorOnCollection<League> navigator = ElideNavigator.of(League.class)
                                                                 .collection()
                                                                 .setFilter(qBuilder().bool("enabled").isTrue());
    return fafApiAccessor.getMany(navigator)
                         .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
                         .cache();
  }

  @Cacheable(value = CacheNames.DIVISIONS, sync = true)
  public Flux<LeagueSeasonBean> getActiveSeasons() {
    ElideNavigatorOnCollection<LeagueSeason> navigator = ElideNavigator.of(LeagueSeason.class)
                                                                       .collection()
                                                                       .setFilter(qBuilder().instant("startDate")
                                                                                            .before(OffsetDateTime.now()
                                                                                                                  .toInstant(),
                                                                                                    false)
                                                                                            .and()
                                                                                            .instant("endDate")
                                                                                            .after(OffsetDateTime.now()
                                                                                                                 .toInstant(),
                                                                                                   false));
    return fafApiAccessor.getMany(navigator)
                         .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
                         .cache();
  }

  @Cacheable(value = CacheNames.LEAGUE, sync = true)
  public Mono<LeagueSeasonBean> getLatestSeason(LeagueBean league) {
    ElideNavigatorOnCollection<LeagueSeason> navigator = ElideNavigator.of(LeagueSeason.class)
                                                                       .collection()
                                                                       .setFilter(qBuilder().intNum("league.id")
                                                                                            .eq(league.id())
                                                                                            .and()
                                                                                            .instant("startDate")
                                                                                            .before(OffsetDateTime.now()
                                                                                                                  .toInstant(),
                                                                                                    false))
                                                                       .addSortingRule("startDate", false);
    return fafApiAccessor.getMany(navigator)
                         .next()
                         .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()));
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public Mono<LeagueEntryBean> getLeagueEntryForPlayer(PlayerBean player, LeagueSeasonBean leagueSeason) {
    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class)
                                                                            .collection()
                                                                            .setFilter(qBuilder().intNum("loginId")
                                                                                                 .eq(player.getId())
                                                                                                 .and()
                                                                                                 .intNum(
                                                                                                     "leagueSeason.id")
                                                                                                 .eq(leagueSeason.id()));
    return fafApiAccessor.getMany(navigator)
                         .next()
                         .map(dto -> leaderboardMapper.map(dto, player, null, new CycleAvoidingMappingContext()))
                         .cache();
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public Mono<LeagueEntryBean> getHighestActiveLeagueEntryForPlayer(PlayerBean player) {
    Condition<?> filter = qBuilder().intNum("loginId")
                                    .eq(player.getId())
                                    .and()
                                    .instant("leagueSeason.startDate")
                                    .before(OffsetDateTime.now().toInstant(), false)
                                    .and()
                                    .instant("leagueSeason.endDate")
                                    .after(OffsetDateTime.now().toInstant(), false);

    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class)
                                                                            .collection()
                                                                            .setFilter(filter);
    return fafApiAccessor.getMany(navigator)
                         .map(dto -> leaderboardMapper.map(dto, player, null, new CycleAvoidingMappingContext()))
                         .filter(leagueEntryBean -> leagueEntryBean.subdivision() != null)
                         .sort(Comparator.comparing(LeagueEntryBean::subdivision,
                                                    Comparator.comparing(SubdivisionBean::division,
                                                                         Comparator.comparing(DivisionBean::index))
                                                              .thenComparing(SubdivisionBean::index)))
                         .takeLast(1)
                         .next()
                         .cache();
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public Mono<LeagueEntryBean> getActiveLeagueEntryForPlayer(PlayerBean player, String leaderboardName) {
    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class)
                                                                            .collection()
                                                                            .setFilter(qBuilder().intNum("loginId")
                                                                                                 .eq(player.getId())
                                                                                                 .and()
                                                                                                 .string(
                                                                                                     "leagueSeason.leagueLeaderboard.technicalName")
                                                                                                 .eq(leaderboardName)
                                                                                                 .and()
                                                                                                 .instant(
                                                                                                     "leagueSeason.startDate")
                                                                                                 .before(
                                                                                                     OffsetDateTime.now()
                                                                                                                   .toInstant(),
                                                                                                     false)
                                                                                                 .and()
                                                                                                 .instant(
                                                                                                     "leagueSeason.endDate")
                                                                                                 .after(
                                                                                                     OffsetDateTime.now()
                                                                                                                   .toInstant(),
                                                                                                     false));
    return fafApiAccessor.getMany(navigator)
                         .filter(leagueEntry -> leagueEntry.getLeagueSeasonDivisionSubdivision() != null)
                         .next()
                         .map(dto -> leaderboardMapper.map(dto, player, null, new CycleAvoidingMappingContext()))
                         .cache();
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public Flux<LeagueEntryBean> getActiveEntries(LeagueSeasonBean leagueSeason) {
    Condition<?> filter = qBuilder().intNum("leagueSeason.id").eq(leagueSeason.id()).and().intNum("score").gte(0);

    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class)
                                                                            .collection()
                                                                            .setFilter(filter)
                                                                            .addSortingRule(
                                                                                "leagueSeasonDivisionSubdivision.leagueSeasonDivision.divisionIndex",
                                                                                false)
                                                                            .addSortingRule(
                                                                                "leagueSeasonDivisionSubdivision.subdivisionIndex",
                                                                                false)
                                                                            .pageSize(fafApiAccessor.getMaxPageSize());

    return fafApiAccessor.getMany(navigator).index()
                         .collectList()
                         .flatMapMany(this::mapLeagueEntryDtoToBean)
                         .cache();
  }

  private Flux<LeagueEntryBean> mapLeagueEntryDtoToBean(List<Tuple2<Long, LeagueSeasonScore>> seasonScoresWithRank) {
    Map<Integer, Tuple2<Long, LeagueSeasonScore>> scoresByPlayer = seasonScoresWithRank.stream()
                                                                                       .collect(Collectors.toMap(
                                                                                           tuple -> tuple.getT2()
                                                                                                         .getLoginId(),
                                                                                      Function.identity()));
    return playerService.getPlayersByIds(scoresByPlayer.keySet()).map(player -> {
      Tuple2<Long, LeagueSeasonScore> seasonScoreWithRank = scoresByPlayer.get(player.getId());
      return leaderboardMapper.map(seasonScoreWithRank.getT2(), player, seasonScoreWithRank.getT1(),
                                   new CycleAvoidingMappingContext());
    });
  }

  @Cacheable(value = CacheNames.DIVISIONS, sync = true)
  public Flux<SubdivisionBean> getAllSubdivisions(LeagueSeasonBean leagueSeason) {
    ElideNavigatorOnCollection<LeagueSeasonDivisionSubdivision> navigator = ElideNavigator.of(
                                                                                              LeagueSeasonDivisionSubdivision.class)
                                                                                          .collection()
                                                                                          .setFilter(qBuilder().string(
                                                                                                                   "leagueSeasonDivision.leagueSeason.id")
                                                                                                               .eq(String.valueOf(
                                                                                                                   leagueSeason.id())));
    return fafApiAccessor.getMany(navigator)
                         .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
                         .cache();
  }

  @Cacheable(value = CacheNames.DIVISIONS, sync = true)
  public Image loadDivisionImage(URL url) {
    return assetService.loadAndCacheImage(url, Path.of("divisions"));
  }
}
