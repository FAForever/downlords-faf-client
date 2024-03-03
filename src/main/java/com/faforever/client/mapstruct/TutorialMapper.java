package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.Tutorial;
import com.faforever.client.domain.api.TutorialCategory;
import org.mapstruct.Mapper;

@Mapper(uses = {MapMapper.class}, config = MapperConfiguration.class)
public interface TutorialMapper {
  Tutorial map(com.faforever.commons.api.dto.Tutorial dto);

  com.faforever.commons.api.dto.Tutorial map(Tutorial bean);

  TutorialCategory map(com.faforever.commons.api.dto.TutorialCategory dto);

  com.faforever.commons.api.dto.TutorialCategory map(TutorialCategory bean);
}
