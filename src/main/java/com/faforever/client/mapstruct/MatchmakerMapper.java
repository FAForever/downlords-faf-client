package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.MapPool;
import com.faforever.client.domain.api.MapPoolAssignment;
import com.faforever.client.domain.api.MatchmakerQueueMapPool;
import com.faforever.client.domain.server.MatchmakerQueueInfo;
import com.faforever.commons.api.dto.MatchmakerQueue;
import com.faforever.commons.lobby.MatchmakerInfo;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(uses = {MapMapper.class, LeaderboardMapper.class}, config = MapperConfiguration.class)
public interface MatchmakerMapper {
    MatchmakerQueueInfo map(MatchmakerQueue dto, @Context CycleAvoidingMappingContext context);

    MatchmakerQueue map(MatchmakerQueueInfo bean, @Context CycleAvoidingMappingContext context);

    @Mapping(target = "playersInQueue", source = "numberOfPlayers")
    @Mapping(target = "queuePopTime", source = "popTime")
    @Mapping(target = "technicalName", source = "name")
    MatchmakerQueueInfo update(MatchmakerInfo.MatchmakerQueue dto, @MappingTarget MatchmakerQueueInfo bean);

    MapPoolAssignment map(com.faforever.commons.api.dto.MapPoolAssignment dto,
                          @Context CycleAvoidingMappingContext context);

    com.faforever.commons.api.dto.MapPoolAssignment map(MapPoolAssignment bean,
                                                        @Context CycleAvoidingMappingContext context);

    List<MapPoolAssignment> mapAssignmentDtos(List<com.faforever.commons.api.dto.MapPoolAssignment> dto,
                                              @Context CycleAvoidingMappingContext context);

    List<com.faforever.commons.api.dto.MapPoolAssignment> mapAssignmentBeans(List<MapPoolAssignment> bean,
                                                                             @Context CycleAvoidingMappingContext context);

    MapPool map(com.faforever.commons.api.dto.MapPool dto, @Context CycleAvoidingMappingContext context);

    com.faforever.commons.api.dto.MapPool map(MapPool bean, @Context CycleAvoidingMappingContext context);

    MatchmakerQueueMapPool map(com.faforever.commons.api.dto.MatchmakerQueueMapPool dto,
                               @Context CycleAvoidingMappingContext context);

    com.faforever.commons.api.dto.MatchmakerQueueMapPool map(MatchmakerQueueMapPool bean,
                                                             @Context CycleAvoidingMappingContext context);
}
