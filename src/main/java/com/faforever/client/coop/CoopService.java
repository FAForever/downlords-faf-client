package com.faforever.client.coop;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.domain.CoopMissionBean;
import com.faforever.client.mapstruct.CoopMapper;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.commons.api.dto.CoopMission;
import com.faforever.commons.api.dto.CoopResult;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;


@Lazy
@Service
@RequiredArgsConstructor
public class CoopService {

  private final FafApiAccessor fafApiAccessor;
  private final CoopMapper coopMapper;

  @Cacheable(value = CacheNames.COOP_MAPS, sync = true)
  public CompletableFuture<List<CoopMissionBean>> getMissions() {
    ElideNavigatorOnCollection<CoopMission> navigator = ElideNavigator.of(CoopMission.class).collection()
        .pageSize(1000);
    return fafApiAccessor.getMany(navigator)
        .map(dto -> coopMapper.map(dto, new CycleAvoidingMappingContext()))
        .collectList()
        .toFuture();
  }

  @Cacheable(value = CacheNames.COOP_LEADERBOARD, sync = true)
  public CompletableFuture<List<CoopResult>> getLeaderboard(CoopMissionBean mission, int numberOfPlayers) {
    Condition<?> filterCondition = qBuilder().intNum("mission").eq(mission.getId());
    if (numberOfPlayers > 0) {
      filterCondition = filterCondition.and().intNum("playerCount").eq(numberOfPlayers);
    }
    ElideNavigatorOnCollection<CoopResult> navigator = ElideNavigator.of(CoopResult.class).collection()
        .setFilter(filterCondition)
        .addSortingRule("duration", true)
        .pageSize(1000);
    return fafApiAccessor.getMany(navigator).collectList().toFuture();
  }
}
