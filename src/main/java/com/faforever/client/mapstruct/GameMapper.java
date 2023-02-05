package com.faforever.client.mapstruct;

import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fa.GameParameters;
import com.faforever.client.player.PlayerService;
import com.faforever.client.util.TimeUtil;
import com.faforever.commons.lobby.GameInfo;
import com.faforever.commons.lobby.GameInfo.TeamIds;
import com.faforever.commons.lobby.GameLaunchResponse;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE, config = MapperConfiguration.class)
public abstract class GameMapper {

  @Autowired
  private PlayerService playerService;

  @Mapping(target = "additionalArgs", source = "args")
  public abstract GameParameters map(GameLaunchResponse dto);

  @Mapping(target = "id", source = "uid")
  @Mapping(target = "status", source = "state")
  @Mapping(target = "mapFolderName", source = "mapName")
  @Mapping(target = "enforceRating", source = "enforceRatingRange")
  @Mapping(target = "startTime", source = "launchedAt")
  @Mapping(target = "teams", source = "teamIds")
  public abstract GameBean update(GameInfo dto, @MappingTarget GameBean bean);

  public Map<Integer, Set<PlayerBean>> map(List<TeamIds> teamIds) {
    if (teamIds == null || teamIds.isEmpty()) {
      return Map.of();
    }
    return teamIds.stream()
        .collect(Collectors.toMap(TeamIds::getTeamId, teamIds1 -> teamIds1.getPlayerIds()
            .stream()
            .map(playerService::getPlayerByIdIfOnline)
            .flatMap(Optional::stream)
            .collect(Collectors.toSet())));
  }

  public OffsetDateTime mapLaunchedAt(Double launchedAt) {
    if (launchedAt == null) {
      return null;
    }
    return TimeUtil.fromPythonTime(launchedAt.longValue());
  }
}
