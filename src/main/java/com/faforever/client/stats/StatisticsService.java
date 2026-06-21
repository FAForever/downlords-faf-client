package com.faforever.client.stats;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.domain.api.Leaderboard;
import com.faforever.client.domain.api.LeaderboardRatingJournal;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.mapstruct.LeaderboardMapper;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;


@Lazy
@Service
@RequiredArgsConstructor
public class StatisticsService {

  private final FafApiAccessor fafApiAccessor;
  private final LeaderboardMapper leaderboardMapper;

  @Cacheable(value = CacheNames.RATING_HISTORY, sync = true)
  public Flux<LeaderboardRatingJournal> getRatingHistory(PlayerInfo player, Leaderboard leaderboard,
                                                         @Nullable OffsetDateTime since) {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.LeaderboardRatingJournal> navigator = ElideNavigator.of(
                                                                                                                     com.faforever.commons.api.dto.LeaderboardRatingJournal.class)
                                                                                                                 .collection()
                                                                                                                 .setFilter(
                                                                                                                     since != null
                                                                                                                         ? qBuilder().instant(
                                                                                                                                         "gamePlayerStats.scoreTime")
                                                                                                                                     .after(
                                                                                                                                         since.toInstant(),
                                                                                                                                         false)
                                                                                                                                     .and()
                                                                                                                                     .intNum(
                                                                                                                                         "gamePlayerStats.player.id")
                                                                                                                                     .eq(player.getId())
                                                                                                                                     .and()
                                                                                                                                     .intNum(
                                                                                                                                         "leaderboard.id")
                                                                                                                                     .eq(leaderboard.id())
                                                                                                                         : qBuilder().intNum(
                                                                                                                                         "gamePlayerStats.player.id")
                                                                                                                                     .eq(player.getId())
                                                                                                                                     .and()
                                                                                                                                     .intNum(
                                                                                                                                         "leaderboard.id")
                                                                                                                                     .eq(leaderboard.id()))
                                                                                                                 .pageSize(
                                                                                                                     fafApiAccessor.getMaxPageSize());
    return fafApiAccessor.getAll(navigator).map(leaderboardMapper::map).cache();
  }
}
