package com.faforever.client.mapstruct;

import com.faforever.client.domain.ModerationReportBean;
import com.faforever.commons.api.dto.ModerationReport;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PlayerMapper.class, ReplayMapper.class})
public interface ModerationReportMapper {
	@Mapping(target = "gameIncidentTimeCode", source = "gameIncidentTimecode")
	ModerationReportBean map(ModerationReport dto, @Context CycleAvoidingMappingContext context);

	@Mapping(target = "gameIncidentTimecode", source = "gameIncidentTimeCode")
	ModerationReport map(ModerationReportBean bean, @Context CycleAvoidingMappingContext context);

	List<ModerationReportBean> mapDtos(List<ModerationReport> dtos, @Context CycleAvoidingMappingContext context);

	List<ModerationReport> mapBeans(List<ModerationReportBean> beans, @Context CycleAvoidingMappingContext context);
}