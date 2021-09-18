package com.faforever.client.mapstruct;

import com.faforever.client.domain.VotingChoiceBean;
import com.faforever.client.domain.VotingQuestionBean;
import com.faforever.client.domain.VotingSubjectBean;
import com.faforever.commons.api.dto.VotingChoice;
import com.faforever.commons.api.dto.VotingQuestion;
import com.faforever.commons.api.dto.VotingSubject;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", config = MapperConfiguration.class)
public interface VotingMapper {
     VotingChoiceBean map(VotingChoice dto, @Context CycleAvoidingMappingContext context);

     VotingChoice map(VotingChoiceBean bean, @Context CycleAvoidingMappingContext context);

     VotingQuestionBean map(VotingQuestion dto, @Context CycleAvoidingMappingContext context);

     VotingQuestion map(VotingQuestionBean bean, @Context CycleAvoidingMappingContext context);

     VotingSubjectBean map(VotingSubject dto, @Context CycleAvoidingMappingContext context);

     VotingSubject map(VotingSubjectBean bean, @Context CycleAvoidingMappingContext context);
}
