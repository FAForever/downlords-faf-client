package com.faforever.client.mapstruct;

import com.faforever.client.domain.CoopMissionBean;
import com.faforever.client.domain.CoopResultBean;
import com.faforever.commons.api.dto.CoopMission;
import com.faforever.commons.api.dto.CoopResult;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ReplayMapper.class})
public interface CoopMapper {

    @Mapping(target="mapFolderName", source="folderName")
    CoopMissionBean map(CoopMission dto, @Context CycleAvoidingMappingContext context);

    @Mapping(target="folderName", source="mapFolderName")
    CoopMission map(CoopMissionBean bean, @Context CycleAvoidingMappingContext context);

    @Mapping(target="replay", source="game")
    CoopResultBean map(CoopResult dto, @Context CycleAvoidingMappingContext context);

    @Mapping(target="game", source="replay")
    CoopResult map(CoopResultBean bean, @Context CycleAvoidingMappingContext context);
}