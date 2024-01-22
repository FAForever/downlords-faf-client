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

  private static final Comparator<SubdivisionBean> SUBDIVISION_COMPARATOR = Comparator.comparing(
                                                                                          SubdivisionBean::getDivision, Comparator.comparing(DivisionBean::getIndex))
                                                                                      .thenComparing(
                                                                                          SubdivisionBean::getIndex);

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
                                                                                            .eq(league.getId())
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

  @Cacheable(value = CacheNames.LEAGUE, sync = true)
  public Mono<Integer> getPlayerNumberInHigherDivisions(SubdivisionBean subdivision) {
    LeagueSeasonBean leagueSeason = subdivision.getDivision().getLeagueSeason();
    return getAllSubdivisions(leagueSeason).filter(
                                               otherSubdivision -> SUBDIVISION_COMPARATOR.compare(otherSubdivision, subdivision) > 0)
                                           .flatMap(this::getSizeOfDivision)
                                           .reduce(0, Integer::sum)
                                           .cache();
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public Mono<Integer> getTotalPlayers(LeagueSeasonBean leagueSeason) {
    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class)
                                                                            .collection()
                                                                            .setFilter(
                                                                                qBuilder().intNum("leagueSeason.id")
                                                                                          .eq(leagueSeason.getId())
                                                                                          .and()
                                                                                          .intNum("score")
                                                                                          .gt(-1))
                                                                            .pageSize(1);
    return fafApiAccessor.getManyWithPageCount(navigator).map(Tuple2::getT2);
  }

  @Cacheable(value = CacheNames.DIVISIONS, sync = true)
  public Mono<Integer> getSizeOfDivision(SubdivisionBean subdivision) {
    Condition<?> filter = qBuilder().intNum("leagueSeason.id")
                                    .eq(subdivision.getDivision().getLeagueSeason().getId())
                                    .and()
                                    .intNum("leagueSeasonDivisionSubdivision.subdivisionIndex")
                                    .eq(subdivision.getIndex())
                                    .and()
                                    .intNum("leagueSeasonDivisionSubdivision.leagueSeasonDivision.divisionIndex")
                                    .eq(subdivision.getDivision().getIndex());

    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class)
                                                                            .collection()
                                                                            .setFilter(filter)
                                                                            .pageSize(1);

    return fafApiAccessor.getManyWithPageCount(navigator).map(Tuple2::getT2);
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
                                                                                                 .eq(leagueSeason.getId()));
    return fafApiAccessor.getMany(navigator)
                         .next()
                         .map(dto -> leaderboardMapper.map(dto, player, new CycleAvoidingMappingContext()))
                         .cache();
  }

  public Mono<LeagueEntryBean> getHighestActiveLeagueEntryForPlayer(PlayerBean player) {
    return getActiveLeagueEntriesForPlayer(player).sort(
        Comparator.comparing(LeagueEntryBean::getSubdivision, SUBDIVISION_COMPARATOR)).takeLast(1).next();
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public Mono<LeagueEntryBean> getActiveLeagueEntryForPlayer(PlayerBean player, String leaderboardName) {
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
                         .next()
                         .map(dto -> leaderboardMapper.map(dto, player, new CycleAvoidingMappingContext()))
                         .filter(leagueEntryBean -> leagueEntryBean.getSubdivision() != null)
                         .cache();
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public Flux<LeagueEntryBean> getActiveLeagueEntriesForPlayer(PlayerBean player) {
    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class)
                                                                            .collection()
                                                                            .setFilter(qBuilder().intNum("loginId")
                                                                                                 .eq(player.getId())
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
                         .map(dto -> leaderboardMapper.map(dto, player, new CycleAvoidingMappingContext()))
                         .filter(leagueEntryBean -> leagueEntryBean.getSubdivision() != null)
                         .cache();
  }

  @Cacheable(value = CacheNames.LEAGUE_ENTRIES, sync = true)
  public Flux<LeagueEntryBean> getEntries(SubdivisionBean subdivision) {
    Condition<?> filter = qBuilder().intNum("leagueSeason.id")
                                    .eq(subdivision.getDivision().getLeagueSeason().getId())
                                    .and()
                                    .intNum("leagueSeasonDivisionSubdivision.subdivisionIndex")
                                    .eq(subdivision.getIndex())
                                    .and()
                                    .intNum("leagueSeasonDivisionSubdivision.leagueSeasonDivision.divisionIndex")
                                    .eq(subdivision.getDivision().getIndex());

    ElideNavigatorOnCollection<LeagueSeasonScore> navigator = ElideNavigator.of(LeagueSeasonScore.class)
                                                                            .collection()
                                                                            .setFilter(filter)
                                                                            .addSortingRule("score", false);

    return fafApiAccessor.getMany(navigator).collectList().flatMapMany(this::mapLeagueEntryDtoToBean).cache();
  }

  private Flux<LeagueEntryBean> mapLeagueEntryDtoToBean(List<LeagueSeasonScore> seasonScores) {
    Map<Integer, LeagueSeasonScore> scoresMap = seasonScores.stream()
                                                            .collect(Collectors.toMap(LeagueSeasonScore::getLoginId,
                                                                                      Function.identity()));
    return playerService.getPlayersByIds(scoresMap.keySet())
                        .map(player -> leaderboardMapper.map(scoresMap.get(player.getId()), player,
                                                             new CycleAvoidingMappingContext()));
  }

  @Cacheable(value = CacheNames.DIVISIONS, sync = true)
  public Flux<SubdivisionBean> getAllSubdivisions(LeagueSeasonBean leagueSeason) {
    ElideNavigatorOnCollection<LeagueSeasonDivisionSubdivision> navigator = ElideNavigator.of(
                                                                                              LeagueSeasonDivisionSubdivision.class)
                                                                                          .collection()
                                                                                          .setFilter(qBuilder().string(
                                                                                                                   "leagueSeasonDivision.leagueSeason.id")
                                                                                                               .eq(String.valueOf(
                                                                                                                   leagueSeason.getId())));
    return fafApiAccessor.getMany(navigator)
                         .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext()))
                         .cache();
  }

  @Cacheable(value = CacheNames.DIVISIONS, sync = true)
  public Image loadDivisionImage(URL url) {
    return assetService.loadAndCacheImage(url, Path.of("divisions"));
  }
}
