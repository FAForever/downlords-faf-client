package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.Map;
import com.faforever.client.domain.api.MapType;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.map.MapSize;
import com.faforever.commons.api.dto.MapPoolAssignment;
import com.faforever.commons.api.dto.NeroxisGeneratorParams;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(uses = {PlayerMapper.class, ReviewMapper.class}, config = MapperConfiguration.class)
public interface MapMapper {
  @Mapping(target = "reviewsSummary", source = "mapReviewsSummary")
  Map map(com.faforever.commons.api.dto.Map dto);

  com.faforever.commons.api.dto.Map map(Map bean);

  List<Map> mapMapDtos(List<com.faforever.commons.api.dto.Map> dto);

  List<com.faforever.commons.api.dto.Map> mapMapBeans(List<Map> bean);

  @Mapping(target = "size", expression = "java(getMapSize(dto))")
  MapVersion map(com.faforever.commons.api.dto.MapVersion dto);

  @Mapping(target = "width", source = "size.widthInPixels")
  @Mapping(target = "height", source = "size.heightInPixels")
  com.faforever.commons.api.dto.MapVersion map(MapVersion bean);

  default MapSize getMapSize(com.faforever.commons.api.dto.MapVersion dto) {
    if (dto.getWidth() == null || dto.getHeight() == null) {
      return null;
    }
    return new MapSize(dto.getWidth(), dto.getHeight());
  }

  List<MapVersion> mapVersionDtos(List<com.faforever.commons.api.dto.MapVersion> dto);

  List<com.faforever.commons.api.dto.MapVersion> mapVersionBeans(List<MapVersion> bean);

  default MapType mapStringToMapType(String string) {
    return MapType.fromValue(string);
  }

  default String mapMapTypeToString(MapType mapType) {
    return mapType.getValue();
  }

  default MapVersion mapFromPoolAssignment(MapPoolAssignment dto) {
    if (dto.getMapVersion() != null) {
      return map(dto.getMapVersion());
    } else if (dto.getMapParams() instanceof NeroxisGeneratorParams neroxisGeneratorParams) {
      return map(neroxisGeneratorParams);
    } else {
      return null;
    }
  }

  default MapVersion map(NeroxisGeneratorParams params) {
    int maxPlayers = params.getSpawns();
    String folderName = String.format("neroxis_map_generator_%s_mapSize=%dkm_spawns=%d", params.getVersion(),
                                      (int) (params.getSize() / 51.2), maxPlayers);
    Map map = new Map(null, folderName, 0, null, false, MapType.SKIRMISH, null);
    MapSize mapSize = new MapSize(params.getSize(), params.getSize());
    return new MapVersion(null, folderName, 0, null, maxPlayers, mapSize, null, false, true, null, null, null, map,
                          null);
  }
}
