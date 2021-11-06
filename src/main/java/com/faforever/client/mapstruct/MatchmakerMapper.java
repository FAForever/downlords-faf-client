package com.faforever.client.mapstruct;

import com.faforever.client.domain.MapPoolAssignmentBean;
import com.faforever.client.domain.MapPoolBean;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.MatchmakerQueueMapPoolBean;
import com.faforever.commons.api.dto.MapPool;
import com.faforever.commons.api.dto.MapPoolAssignment;
import com.faforever.commons.api.dto.MatchmakerQueue;
import com.faforever.commons.api.dto.MatchmakerQueueMapPool;
import com.faforever.commons.lobby.MatchmakerInfo;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {MapMapper.class, LeaderboardMapper.class}, config = MapperConfiguration.class)
public interface MatchmakerMapper {
    MatchmakerQueueBean map(MatchmakerQueue dto, @Context CycleAvoidingMappingContext context);
    MatchmakerQueue map(MatchmakerQueueBean bean, @Context CycleAvoidingMappingContext context);

    @Mapping(target = "playersInQueue", source = "numberOfPlayers")
    @Mapping(target = "queuePopTime", source = "popTime")
    MatchmakerQueueBean update(MatchmakerInfo.MatchmakerQueue dto, @MappingTarget MatchmakerQueueBean bean);

    MapPoolAssignmentBean map(MapPoolAssignment dto, @Context CycleAvoidingMappingContext context);
    MapPoolAssignment map(MapPoolAssignmentBean bean, @Context CycleAvoidingMappingContext context);

    List<MapPoolAssignmentBean> mapAssignmentDtos(List<MapPoolAssignment> dto, @Context CycleAvoidingMappingContext context);
    List<MapPoolAssignment> mapAssignmentBeans(List<MapPoolAssignmentBean> bean, @Context CycleAvoidingMappingContext context);

    MapPoolBean map(MapPool dto, @Context CycleAvoidingMappingContext context);
    MapPool map(MapPoolBean bean, @Context CycleAvoidingMappingContext context);

    MatchmakerQueueMapPoolBean map(MatchmakerQueueMapPool dto, @Context CycleAvoidingMappingContext context);
    MatchmakerQueueMapPool map(MatchmakerQueueMapPoolBean bean, @Context CycleAvoidingMappingContext context);
}
