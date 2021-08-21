package com.faforever.client.mapstruct;

import com.faforever.client.domain.TutorialBean;
import com.faforever.client.domain.TutorialCategoryBean;
import com.faforever.commons.api.dto.Tutorial;
import com.faforever.commons.api.dto.TutorialCategory;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {MapMapper.class})
public interface TutorialMapper {
     TutorialBean map(Tutorial dto, @Context CycleAvoidingMappingContext context);

     Tutorial map(TutorialBean bean, @Context CycleAvoidingMappingContext context);

     TutorialCategoryBean map(TutorialCategory dto, @Context CycleAvoidingMappingContext context);

     TutorialCategory map(TutorialCategoryBean bean, @Context CycleAvoidingMappingContext context);
}
