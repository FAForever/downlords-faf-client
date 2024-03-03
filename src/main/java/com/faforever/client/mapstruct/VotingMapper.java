package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.VotingChoice;
import com.faforever.client.domain.api.VotingQuestion;
import com.faforever.client.domain.api.VotingSubject;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfiguration.class)
public interface VotingMapper {
  VotingChoice map(com.faforever.commons.api.dto.VotingChoice dto);

  com.faforever.commons.api.dto.VotingChoice map(VotingChoice bean, @Context CycleAvoidingMappingContext context);

  VotingQuestion map(com.faforever.commons.api.dto.VotingQuestion dto);

  com.faforever.commons.api.dto.VotingQuestion map(VotingQuestion bean, @Context CycleAvoidingMappingContext context);

  VotingSubject map(com.faforever.commons.api.dto.VotingSubject dto);

  com.faforever.commons.api.dto.VotingSubject map(VotingSubject bean, @Context CycleAvoidingMappingContext context);
}
