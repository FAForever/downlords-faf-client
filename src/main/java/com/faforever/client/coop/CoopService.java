package com.faforever.client.coop;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.domain.api.CoopMission;
import com.faforever.client.domain.api.CoopResult;
import com.faforever.client.mapstruct.CoopMapper;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.GamePlayerStats;
import com.faforever.commons.api.dto.Player;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;


@Lazy
@Service
@RequiredArgsConstructor
public class CoopService {

  private final FafApiAccessor fafApiAccessor;
  private final CoopMapper coopMapper;

  @Cacheable(value = CacheNames.COOP_MAPS, sync = true)
  public Flux<CoopMission> getMissions() {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.CoopMission> navigator = ElideNavigator.of(
        com.faforever.commons.api.dto.CoopMission.class).collection().pageSize(1000);
    return fafApiAccessor.getMany(navigator).map(dto -> coopMapper.map(dto, new CycleAvoidingMappingContext())).cache();
  }

  @Cacheable(value = CacheNames.COOP_LEADERBOARD, sync = true)
  public Flux<CoopResult> getLeaderboard(CoopMission mission, int numberOfPlayers) {
    Condition<?> filterCondition = qBuilder().intNum("mission").eq(mission.id());
    if (numberOfPlayers > 0) {
      filterCondition = filterCondition.and().intNum("playerCount").eq(numberOfPlayers);
    }
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.CoopResult> navigator = ElideNavigator.of(
                                                                                                       com.faforever.commons.api.dto.CoopResult.class)
                                                                                                   .collection()
                                                                                                   .setFilter(
                                                                                                       filterCondition)
                                                                                                   .addSortingRule(
                                                                                                       "duration", true)
                                                                                                   .pageSize(1000);
    return fafApiAccessor.getMany(navigator)
                         .distinct(this::getAllPlayerNamesFromTeams)
                         .index(
                             (index, dto) -> coopMapper.map(dto, index.intValue(), new CycleAvoidingMappingContext()))
                         .cache();
  }

  private Set<String> getAllPlayerNamesFromTeams(com.faforever.commons.api.dto.CoopResult coopResult) {
    Game game = coopResult.getGame();
    List<GamePlayerStats> playerStats = game == null ? null : game.getPlayerStats();
    return playerStats == null ? Set.of() : playerStats.stream()
                                                       .map(GamePlayerStats::getPlayer)
                                                       .map(Player::getLogin)
                                                       .collect(Collectors.toUnmodifiableSet());
  }
}
