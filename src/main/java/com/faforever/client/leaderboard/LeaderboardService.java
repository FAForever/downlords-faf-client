package com.faforever.client.leaderboard;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.domain.api.Division;
import com.faforever.client.domain.api.Leaderboard;
import com.faforever.client.domain.api.LeaderboardEntry;
import com.faforever.client.domain.api.League;
import com.faforever.client.domain.api.LeagueEntry;
import com.faforever.client.domain.api.LeagueSeason;
import com.faforever.client.domain.api.Subdivision;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.mapstruct.LeaderboardMapper;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
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
  public Flux<Leaderboard> getLeaderboards() {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.Leaderboard> navigator = ElideNavigator.of(
        com.faforever.commons.api.dto.Leaderboard.class).collection();
    return fafApiAccessor.getMany(navigator).map(leaderboardMapper::map)
                         .cache();
  }

  @Cacheable(value = CacheNames.LEADERBOARD, sync = true)
  public Flux<LeaderboardEntry> getEntriesForPlayer(PlayerInfo player) {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.LeaderboardEntry> navigator = ElideNavigator.of(
                                                                                                             com.faforever.commons.api.dto.LeaderboardEntry.class)
                                                                                                         .collection()
                                                                                                         .setFilter(
                                                                                                             qBuilder().intNum(
                                                                                                                           "player.id")
                                                                                                                       .eq(player.getId()));
    return fafApiAccessor.getMany(navigator).map(leaderboardMapper::map)
                         .cache();
  }

  @Cacheable(value = CacheNames.LEAGUE, sync = true)
  public Flux<League> getLeagues() {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.League> navigator = ElideNavigator.of(
        com.faforever.commons.api.dto.League.class).collection().setFilter(qBuilder().bool("enabled").isTrue());
    return fafApiAccessor.getMany(navigator).map(leaderboardMapper::map)
                         .cache();
  }

  @Cacheable(value = CacheNames.DIVISIONS, sync = true)
  public Flux<LeagueSeason> getActiveSeasons() {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.LeagueSeason> navigator = ElideNavigator.of(
                                                                                                         com.faforever.commons.api.dto.LeagueSeason.class)
                                                                                                     .collection()
                                                                                                     .setFilter(
                                                                                                         qBuilder().instant(
                                                                                                                       "startDate")
                                                                                                                   .before(
                                                                                                                       OffsetDateTime.now()
                                                                                                                                     .toInstant(),
                                                                                                                       false)
                                                                                                                   .and()
                                                                                                                   .instant(
                                                                                                                       "endDate")
                                                                                                                   .after(
                                                                                                                       OffsetDateTime.now()
                                                                                                                                     .toInstant(),
                                                                                                                       false));
    return fafApiAccessor.getMany(navigator).map(leaderboardMapper::map)
                         .cache();
  }

  @Cacheable(value = CacheNames.LEAGUE, sync = true)
  public Mono<LeagueSeason> getLatestSeason(League league) {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.LeagueSeason> navigator = ElideNavigator.of(
                                                                                                         com.faforever.commons.api.dto.LeagueSeason.class)
                                                                                                     .collection()
                                                                                                     .setFilter(
                                                                                                         qBuilder().intNum(
                                                                                                                       "league.id")
                                                                                                                   .eq(league.id())
                                                                                                                   .and()
                                                                                                                   .instant(
                                                                                                                       "startDate")
                                                                                                                   .before(
                                                                                                                       OffsetDateTime.now()
                                                                                                                                     .toInstant(),
                                                                                                                       false))
                                                                                                     .addSortingRule(
                                                                                                         "startDate",
                                                                                                         false);
    return fafApiAccessor.getMany(navigator)
                         .next().map(leaderboardMapper::map);
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public Mono<LeagueEntry> getLeagueEntryForPlayer(PlayerInfo player, LeagueSeason leagueSeason) {
    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class)
                                                                            .collection()
                                                                            .setFilter(qBuilder().intNum("loginId")
                                                                                                 .eq(player.getId())
                                                                                                 .and()
                                                                                                 .intNum(
                                                                                                     "leagueSeason.id")
                                                                                                 .eq(leagueSeason.id()));
    return fafApiAccessor.getMany(navigator)
                         .next().map(dto -> leaderboardMapper.map(dto, player, null))
                         .cache();
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public Mono<LeagueEntry> getHighestActiveLeagueEntryForPlayer(PlayerInfo player) {
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
    return fafApiAccessor.getMany(navigator).map(dto -> leaderboardMapper.map(dto, player, null))
                         .filter(leagueEntryBean -> leagueEntryBean.subdivision() != null)
                         .sort(Comparator.comparing(LeagueEntry::subdivision,
                                                    Comparator.comparing(Subdivision::division,
                                                                         Comparator.comparing(Division::index))
                                                              .thenComparing(Subdivision::index)))
                         .takeLast(1)
                         .next()
                         .cache();
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public Mono<LeagueEntry> getActiveLeagueEntryForPlayer(PlayerInfo player, String leaderboardName) {
    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class)
                                                                            .collection()
                                                                            .setFilter(qBuilder().intNum("loginId")
                                                                                                 .eq(player.getId())
                                                                                                 .and()
                                                                                                 .string(
                                                                                                     "leagueSeason.leaderboard.technicalName")
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
                         .next().map(dto -> leaderboardMapper.map(dto, player, null))
                         .cache();
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public Flux<LeagueEntry> getActiveEntries(LeagueSeason leagueSeason) {
    Condition<?> filter = qBuilder().intNum("leagueSeason.id").eq(leagueSeason.id()).and().intNum("score").gte(0);

    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class)
                                                                            .collection()
                                                                            .setFilter(filter)
                                                                            .addSortingRule(
                                                                                "leagueSeasonDivisionSubdivision.leagueSeasonDivision.divisionIndex",
                                                                                false)
                                                                            .addSortingRule(
                                                                                "leagueSeasonDivisionSubdivision.subdivisionIndex",
                                                                                false).addSortingRule("score", false)
                                                                            .pageSize(fafApiAccessor.getMaxPageSize());

    return fafApiAccessor.getMany(navigator).index().collectList().flatMapMany(this::mapLeagueEntryDtoToBean).cache();
  }

  private Flux<LeagueEntry> mapLeagueEntryDtoToBean(List<Tuple2<Long, LeagueSeasonScore>> seasonScoresWithRank) {
    Map<Integer, Tuple2<Long, LeagueSeasonScore>> scoresByPlayer = seasonScoresWithRank.stream()
                                                                                       .collect(Collectors.toMap(
                                                                                           tuple -> tuple.getT2()
                                                                                                         .getLoginId(),
                                                                                           Function.identity()));
    return playerService.getPlayersByIds(scoresByPlayer.keySet()).map(player -> {
      Tuple2<Long, LeagueSeasonScore> seasonScoreWithRank = scoresByPlayer.get(player.getId());
      return leaderboardMapper.map(seasonScoreWithRank.getT2(), player, seasonScoreWithRank.getT1());
    }).sort(Comparator.comparing(LeagueEntry::rank));
  }

  @Cacheable(value = CacheNames.DIVISIONS, sync = true)
  public Flux<Subdivision> getAllSubdivisions(LeagueSeason leagueSeason) {
    ElideNavigatorOnCollection<LeagueSeasonDivisionSubdivision> navigator = ElideNavigator.of(
                                                                                              LeagueSeasonDivisionSubdivision.class)
                                                                                          .collection()
                                                                                          .setFilter(qBuilder().string(
                                                                                                                   "leagueSeasonDivision.leagueSeason.id")
                                                                                                               .eq(String.valueOf(
                                                                                                                   leagueSeason.id())));
    return fafApiAccessor.getMany(navigator).map(leaderboardMapper::map)
                         .cache();
  }

  @Cacheable(value = CacheNames.DIVISIONS, sync = true)
  public Image loadDivisionImage(URL url) {
    return assetService.loadAndCacheImage(url, Path.of("divisions"));
  }
}
