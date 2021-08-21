package com.faforever.client.mapstruct;

import com.faforever.client.domain.GameBean;
import com.faforever.client.util.TimeUtil;
import com.faforever.commons.lobby.GameInfo;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE)
public interface GameMapper {
  String OBSERVERS_TEAM = "-1";

  @Mapping(target = "id", source = "uid")
  @Mapping(target = "numPlayers", expression = "java(getNumPlayers(dto))")
  @Mapping(target = "status", source = "state")
  @Mapping(target = "mapFolderName", source = "mapName")
  @Mapping(target = "enforceRating", source = "enforceRatingRange")
  @Mapping(target = "startTime", source = "launchedAt")
  GameBean update(GameInfo dto, @MappingTarget GameBean bean);

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
