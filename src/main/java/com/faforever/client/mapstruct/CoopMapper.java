package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.CoopMission;
import com.faforever.client.domain.api.CoopResult;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {ReplayMapper.class}, config = MapperConfiguration.class)
public interface CoopMapper {

  @Mapping(target = "mapFolderName", source = "folderName")
  CoopMission map(com.faforever.commons.api.dto.CoopMission dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "folderName", source = "mapFolderName")
  com.faforever.commons.api.dto.CoopMission map(CoopMission bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "replay", source = "dto.game")
  @Mapping(target = "ranking", source = "ranking")
  @Mapping(target = ".", source = "dto")
  CoopResult map(com.faforever.commons.api.dto.CoopResult dto, int ranking,
                 @Context CycleAvoidingMappingContext context);

  @Mapping(target = "game", source = "replay")
  com.faforever.commons.api.dto.CoopResult map(CoopResult bean, @Context CycleAvoidingMappingContext context);
}