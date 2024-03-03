package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.Tournament;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = MapperConfiguration.class)
public interface TournamentMapper {
  Tournament map(com.faforever.commons.api.dto.Tournament dto);

  com.faforever.commons.api.dto.Tournament map(Tournament bean);

  List<Tournament> mapDtos(List<com.faforever.commons.api.dto.Tournament> dto);

  List<com.faforever.commons.api.dto.Tournament> mapBeans(List<Tournament> bean);
}
