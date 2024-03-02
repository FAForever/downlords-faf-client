package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.FeaturedMod;
import com.faforever.client.domain.api.ModType;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(imports = ModType.class, config = MapperConfiguration.class)
public interface FeaturedModMapper {
  FeaturedMod map(com.faforever.commons.api.dto.FeaturedMod dto, @Context CycleAvoidingMappingContext context);

  com.faforever.commons.api.dto.FeaturedMod map(FeaturedMod bean, @Context CycleAvoidingMappingContext context);


}
