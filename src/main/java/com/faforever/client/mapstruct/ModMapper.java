package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.Mod;
import com.faforever.client.domain.api.ModType;
import com.faforever.client.domain.api.ModVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Mapper(uses = {PlayerMapper.class, ReviewMapper.class}, config = MapperConfiguration.class)
public interface ModMapper {
    @Mapping(target = "uid", source = "modInfo.uid")
    @Mapping(target = "mod.author", source = "modInfo.author")
    @Mapping(target = "modType", source = "modInfo.uiOnly")
    @Mapping(target = "mod", expression = "java(new ModBean())")
    @Mapping(target = "mod.displayName", source = "modInfo.name")
    ModVersion map(com.faforever.commons.mod.Mod modInfo, Path basePath);

    default ModType mapModType(boolean isUIOnly) {
        return isUIOnly ? ModType.UI : ModType.SIM;
    }

    default Path mapImagePath(com.faforever.commons.mod.Mod modInfo, Path basePath) {
        return Optional.ofNullable(modInfo.getIcon())
            .map(Paths::get)
            .filter(iconPath -> iconPath.getNameCount() > 2)
            .map(iconPath -> basePath.resolve(iconPath.subpath(2, iconPath.getNameCount())))
        .orElse(null);
    }

  @Mapping(target = "reviewsSummary", source = "modReviewsSummary")
  Mod map(com.faforever.commons.api.dto.Mod dto);

  com.faforever.commons.api.dto.Mod map(Mod bean);

    @Mapping(target = "modType", source = "type")
    ModVersion map(com.faforever.commons.api.dto.ModVersion dto);

    @Mapping(target = "type", source = "modType")
    com.faforever.commons.api.dto.ModVersion map(ModVersion bean);
}
