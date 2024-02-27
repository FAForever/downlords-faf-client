package com.faforever.client.stats;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardRatingJournalBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.LeaderboardMapper;
import com.faforever.commons.api.dto.LeaderboardRatingJournal;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;


@Lazy
@Service
@RequiredArgsConstructor
public class StatisticsService {

  private final FafApiAccessor fafApiAccessor;
  private final LeaderboardMapper leaderboardMapper;

  @Cacheable(value = CacheNames.RATING_HISTORY, sync = true)
  public Flux<LeaderboardRatingJournalBean> getRatingHistory(PlayerBean player, LeaderboardBean leaderboard) {
    ElideNavigatorOnCollection<LeaderboardRatingJournal> navigator = ElideNavigator.of(LeaderboardRatingJournal.class).collection()
        .setFilter(qBuilder().intNum("gamePlayerStats.player.id").eq(player.getId()).and()
                             .intNum("leagueLeaderboard.id")
                             .eq(leaderboard.id()))
        .pageSize(fafApiAccessor.getMaxPageSize());
    return fafApiAccessor.getMany(navigator)
        .map(dto -> leaderboardMapper.map(dto, new CycleAvoidingMappingContext())).cache();
  }
}
