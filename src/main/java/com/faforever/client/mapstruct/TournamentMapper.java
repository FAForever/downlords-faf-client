package com.faforever.client.mapstruct;

import com.faforever.client.domain.TournamentBean;
import com.faforever.commons.api.dto.Tournament;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TournamentMapper {
     TournamentBean map(Tournament dto, @Context CycleAvoidingMappingContext context);

     Tournament map(TournamentBean bean, @Context CycleAvoidingMappingContext context);

     List<TournamentBean> mapDtos(List<Tournament> dto, @Context CycleAvoidingMappingContext context);

     List<Tournament> mapBeans(List<TournamentBean> bean, @Context CycleAvoidingMappingContext context);
}
