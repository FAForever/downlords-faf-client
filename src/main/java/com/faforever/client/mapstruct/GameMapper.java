package com.faforever.client.mapstruct;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fa.GameParameters;
import com.faforever.client.util.TimeUtil;
import com.faforever.commons.lobby.GameInfo;
import com.faforever.commons.lobby.GameInfo.TeamIds;
import com.faforever.commons.lobby.GameLaunchResponse;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE, config = MapperConfiguration.class)
public abstract class GameMapper {

  @Mapping(target = "additionalArgs", source = "args")
  public abstract GameParameters map(GameLaunchResponse dto);
  
  @Mapping(target = "status", source = "state")
  @Mapping(target = "mapFolderName", source = "mapName")
  @Mapping(target = "enforceRating", source = "enforceRatingRange")
  @Mapping(target = "startTime", source = "launchedAt")
  @Mapping(target = "teams", source = "teamIds")
  public abstract GameBean update(GameInfo dto, @MappingTarget GameBean bean);

  public Map<Integer, Set<Integer>> map(List<TeamIds> teamIds) {
    if (teamIds == null || teamIds.isEmpty()) {
      return Map.of();
    }
    return teamIds.stream()
        .collect(Collectors.toMap(TeamIds::getTeamId, teamIds1 -> Set.copyOf(teamIds1.getPlayerIds())));
  }

  public OffsetDateTime mapLaunchedAt(Double launchedAt) {
    if (launchedAt == null) {
      return null;
    }
    return TimeUtil.fromPythonTime(launchedAt.longValue());
  }
}
