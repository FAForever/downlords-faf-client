package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.Tournament;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = MapperConfiguration.class)
public interface TournamentMapper {
  Tournament map(com.faforever.commons.api.dto.Tournament dto, @Context CycleAvoidingMappingContext context);

  com.faforever.commons.api.dto.Tournament map(Tournament bean, @Context CycleAvoidingMappingContext context);

  List<Tournament> mapDtos(List<com.faforever.commons.api.dto.Tournament> dto,
                           @Context CycleAvoidingMappingContext context);

  List<com.faforever.commons.api.dto.Tournament> mapBeans(List<Tournament> bean,
                                                          @Context CycleAvoidingMappingContext context);
}
