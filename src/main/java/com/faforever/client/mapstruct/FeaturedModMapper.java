package com.faforever.client.mapstruct;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.ModVersionBean.ModType;
import com.faforever.commons.api.dto.FeaturedMod;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", imports = ModType.class, config = MapperConfiguration.class)
public interface FeaturedModMapper {
    FeaturedModBean map(FeaturedMod dto, @Context CycleAvoidingMappingContext context);

    FeaturedMod map(FeaturedModBean bean, @Context CycleAvoidingMappingContext context);


}
