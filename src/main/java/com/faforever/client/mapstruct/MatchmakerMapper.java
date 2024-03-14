package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.MapPool;
import com.faforever.client.domain.api.MapPoolAssignment;
import com.faforever.client.domain.api.MatchmakerQueueMapPool;
import com.faforever.client.domain.server.MatchmakerQueueInfo;
import com.faforever.commons.api.dto.MatchmakerQueue;
import com.faforever.commons.lobby.MatchmakerInfo;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(uses = {MapMapper.class, LeaderboardMapper.class}, config = MapperConfiguration.class)
public interface MatchmakerMapper {
    MatchmakerQueueInfo map(MatchmakerQueue dto);

    @InheritInverseConfiguration
    MatchmakerQueue map(MatchmakerQueueInfo bean);

    @Mapping(target = "playersInQueue", source = "numberOfPlayers")
    @Mapping(target = "queuePopTime", source = "popTime")
    @Mapping(target = "technicalName", source = "name")
    MatchmakerQueueInfo update(MatchmakerInfo.MatchmakerQueue dto, @MappingTarget MatchmakerQueueInfo bean);
    @Mapping(target = "mapVersion", source = "dto")
    MapPoolAssignment map(com.faforever.commons.api.dto.MapPoolAssignment dto);

    @InheritInverseConfiguration
    com.faforever.commons.api.dto.MapPoolAssignment map(MapPoolAssignment bean);

    List<MapPoolAssignment> mapAssignmentDtos(List<com.faforever.commons.api.dto.MapPoolAssignment> dto);

    List<com.faforever.commons.api.dto.MapPoolAssignment> mapAssignmentBeans(List<MapPoolAssignment> bean);
    @Mapping(target = "mapPool", source = "matchmakerQueueMapPool")
    MapPool map(com.faforever.commons.api.dto.MapPool dto);

    @InheritInverseConfiguration
    com.faforever.commons.api.dto.MapPool map(MapPool bean);

    MatchmakerQueueMapPool map(com.faforever.commons.api.dto.MatchmakerQueueMapPool dto);

    @InheritInverseConfiguration
    com.faforever.commons.api.dto.MatchmakerQueueMapPool map(MatchmakerQueueMapPool bean);
}
