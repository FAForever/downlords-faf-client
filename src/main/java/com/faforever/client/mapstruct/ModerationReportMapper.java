package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.ModerationReport;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(uses = {PlayerMapper.class, ReplayMapper.class}, config = MapperConfiguration.class)
public interface ModerationReportMapper {
	@Mapping(target = "gameIncidentTimeCode", source = "gameIncidentTimecode")
  ModerationReport map(com.faforever.commons.api.dto.ModerationReport dto,
                       @Context CycleAvoidingMappingContext context);

	@Mapping(target = "gameIncidentTimecode", source = "gameIncidentTimeCode")
  com.faforever.commons.api.dto.ModerationReport map(ModerationReport bean,
                                                     @Context CycleAvoidingMappingContext context);

  List<ModerationReport> mapDtos(List<com.faforever.commons.api.dto.ModerationReport> dtos,
                                 @Context CycleAvoidingMappingContext context);

  List<com.faforever.commons.api.dto.ModerationReport> mapBeans(List<ModerationReport> beans,
                                                                @Context CycleAvoidingMappingContext context);
}