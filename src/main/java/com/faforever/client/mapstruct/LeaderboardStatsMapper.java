package com.faforever.client.mapstruct;

import com.faforever.client.player.LeaderboardRating;
import com.faforever.commons.lobby.Player.LeaderboardStats;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {}, config = MapperConfiguration.class)
public interface LeaderboardStatsMapper {
  @Mapping(target = "deviation", source = "rating.deviation")
  @Mapping(target = "mean", source = "rating.mean")
  LeaderboardRating map(LeaderboardStats dto);
}
