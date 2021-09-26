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

@Mapper(componentModel = "spring", uses = {PlayerMapper.class, ReviewMapper.class}, config = MapperConfiguration.class)
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
    return MapSize.valueOf(dto.getWidth(), dto.getHeight());
  }

  List<MapVersionBean> mapVersionDtos(List<MapVersion> dto, @Context CycleAvoidingMappingContext context);

  List<MapVersion> mapVersionBeans(List<MapVersionBean> bean, @Context CycleAvoidingMappingContext context);

  default MapType mapStringToMapType(String string) {
    return MapType.fromString(string);
  }

  default String mapMapTypeToString(MapType mapType) {
    return mapType.getString();
  }

  default MapVersionBean mapFromPoolAssignment(MapPoolAssignment dto, @Context CycleAvoidingMappingContext context) {
    if (dto.getMapVersion() != null) {
      return map(dto.getMapVersion(), context);
    } else if (dto.getMapParams() instanceof NeroxisGeneratorParams) {
      return map((NeroxisGeneratorParams) dto.getMapParams());
    } else {
      return null;
    }
  }

  default MapVersionBean map(NeroxisGeneratorParams params) {
    MapBean mapBean = new MapBean();
    mapBean.setId(-(params.getSpawns() + params.getSize()));
    mapBean.setDisplayName(String.format("neroxis_map_generator_%s_mapSize=%dkm_spawns=%d", params.getVersion(), (int) (params.getSize() / 51.2), params.getSpawns()));
    MapVersionBean mapVersionBean = new MapVersionBean();
    mapVersionBean.setFolderName(mapBean.getDisplayName());
    mapVersionBean.setDescription("");
    mapVersionBean.setSize(MapSize.valueOf(params.getSize(), params.getSize()));
    mapVersionBean.setMaxPlayers(params.getSpawns());
    mapVersionBean.setHidden(false);
    mapVersionBean.setRanked(true);
    mapVersionBean.setMap(mapBean);
    return mapVersionBean;
  }
}
