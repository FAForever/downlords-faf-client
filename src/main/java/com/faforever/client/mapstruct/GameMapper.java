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
import java.util.stream.Collectors;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE, config = MapperConfiguration.class)
public interface GameMapper {
  String OBSERVERS_TEAM = "-1";

  @Mapping(target = "additionalArgs", source = "args")
  GameParameters map(GameLaunchResponse dto);

  @Mapping(target = "id", source = "uid")
  @Mapping(target = "numPlayers", source = "dto")
  @Mapping(target = "status", source = "state")
  @Mapping(target = "mapFolderName", source = "mapName")
  @Mapping(target = "enforceRating", source = "enforceRatingRange")
  @Mapping(target = "startTime", source = "launchedAt")
  @Mapping(target = "teams", source = "teamIds")
  GameBean update(GameInfo dto, @MappingTarget GameBean bean);

  default Map<Integer, List<Integer>> map(List<TeamIds> teamIds) {
    return teamIds.stream().collect(Collectors.toMap(TeamIds::getTeamId, TeamIds::getPlayerIds));
  }

  default OffsetDateTime mapLaunchedAt(Double launchedAt) {
    if (launchedAt == null) {
      return null;
    }
    return TimeUtil.fromPythonTime(launchedAt.longValue());
  }

  default Integer getNumPlayers(GameInfo dto) {
    Integer numPlayers = dto.getNumberOfPlayers();
    Map<String, List<String>> teams = dto.getTeams();
    if (numPlayers == null || teams == null) {
      return 0;
    }
    return numPlayers - teams.getOrDefault(OBSERVERS_TEAM, List.of()).size();
  }

}
