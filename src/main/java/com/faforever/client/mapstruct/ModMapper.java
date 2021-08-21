package com.faforever.client.mapstruct;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.ModBean;
import com.faforever.client.domain.ModVersionBean;
import com.faforever.client.domain.ModVersionBean.ModType;
import com.faforever.commons.api.dto.FeaturedMod;
import com.faforever.commons.api.dto.Mod;
import com.faforever.commons.api.dto.ModVersion;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Mapper(componentModel = "spring", imports = ModType.class, uses = {PlayerMapper.class, ReviewMapper.class})
public interface ModMapper {
    @Mapping(target = "uid", source = "modInfo.uid")
    @Mapping(target = "mod.author", source = "modInfo.author")
    @Mapping(target = "modType", expression = "java(modInfo.isUiOnly() ? ModType.UI : ModType.SIM)")
    @Mapping(target = "mountPoints", source = "modInfo.mountInfos")
    @Mapping(target = "mod", expression = "java(new ModBean())")
    @Mapping(target = "mod.displayName", source = "modInfo.name")
    @Mapping(target = "imagePath", expression = "java(mapImagePath(modInfo, basePath))")
    ModVersionBean map(com.faforever.commons.mod.Mod modInfo, Path basePath, @Context CycleAvoidingMappingContext context);

    default Path mapImagePath(com.faforever.commons.mod.Mod modInfo, Path basePath) {
        return Optional.ofNullable(modInfo.getIcon())
            .map(Paths::get)
            .filter(iconPath -> iconPath.getNameCount() > 2)
            .map(iconPath -> basePath.resolve(iconPath.subpath(2, iconPath.getNameCount())))
        .orElse(null);
    }

    ModBean map(Mod dto, @Context CycleAvoidingMappingContext context);

    Mod map(ModBean bean, @Context CycleAvoidingMappingContext context);

    ModVersionBean map(ModVersion dto, @Context CycleAvoidingMappingContext context);

    ModVersion map(ModVersionBean bean, @Context CycleAvoidingMappingContext context);

    FeaturedModBean map(FeaturedMod dto, @Context CycleAvoidingMappingContext context);

    FeaturedMod map(FeaturedModBean bean, @Context CycleAvoidingMappingContext context);


}
