package com.faforever.client.mapstruct;

import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapBean.MapType;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.map.MapSize;
import com.faforever.commons.api.dto.Map;
import com.faforever.commons.api.dto.MapPoolAssignment;
import com.faforever.commons.api.dto.MapVersion;
import com.faforever.commons.api.dto.NeroxisGeneratorParams;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(uses = {PlayerMapper.class, ReviewMapper.class}, config = MapperConfiguration.class)
public interface MapMapper {
  MapBean map(Map dto, @Context CycleAvoidingMappingContext context);

  Map map(MapBean bean, @Context CycleAvoidingMappingContext context);

  List<MapBean> mapMapDtos(List<Map> dto, @Context CycleAvoidingMappingContext context);

  List<Map> mapMapBeans(List<MapBean> bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "size", expression = "java(getMapSize(dto))")
  MapVersionBean map(MapVersion dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "width", source = "size.widthInPixels")
  @Mapping(target = "height", source = "size.heightInPixels")
  MapVersion map(MapVersionBean bean, @Context CycleAvoidingMappingContext context);

  default MapSize getMapSize(MapVersion dto) {
    if (dto.getWidth() == null || dto.getHeight() == null) {
      return null;
    }
    return new MapSize(dto.getWidth(), dto.getHeight());
  }

  List<MapVersionBean> mapVersionDtos(List<MapVersion> dto, @Context CycleAvoidingMappingContext context);

  List<MapVersion> mapVersionBeans(List<MapVersionBean> bean, @Context CycleAvoidingMappingContext context);

  default MapType mapStringToMapType(String string) {
    return MapType.fromValue(string);
  }

  default String mapMapTypeToString(MapType mapType) {
    return mapType.getValue();
  }

  default MapVersionBean mapFromPoolAssignment(MapPoolAssignment dto, @Context CycleAvoidingMappingContext context) {
    if (dto.getMapVersion() != null) {
      return map(dto.getMapVersion(), context);
    } else if (dto.getMapParams() instanceof NeroxisGeneratorParams neroxisGeneratorParams) {
      return map(neroxisGeneratorParams);
    } else {
      return null;
    }
  }

  default MapVersionBean map(NeroxisGeneratorParams params) {
    int maxPlayers = params.getSpawns();
    String folderName = String.format("neroxis_map_generator_%s_mapSize=%dkm_spawns=%d", params.getVersion(),
                                      (int) (params.getSize() / 51.2), maxPlayers);
    MapBean mapBean = new MapBean(null, folderName, 0, null, false,
                                  MapType.SKIRMISH, null);
    MapSize mapSize = new MapSize(params.getSize(), params.getSize());
    return new MapVersionBean(null, folderName, 0, null, maxPlayers, mapSize, null, false, true, null, null, null,
                              mapBean, null);
  }
}
