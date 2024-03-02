package com.faforever.client.mapstruct;

import com.faforever.client.avatar.Avatar;
import com.faforever.commons.lobby.Player;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(uses = {UrlMapper.class}, config = MapperConfiguration.class)
public interface AvatarMapper {

  @Mapping(target = "description", source = "tooltip")
  Avatar map(com.faforever.commons.api.dto.Avatar dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "tooltip", source = "description")
  com.faforever.commons.api.dto.Avatar map(Avatar bean, @Context CycleAvoidingMappingContext context);

  Avatar map(Player.Avatar dto, @Context CycleAvoidingMappingContext context);

  List<Avatar> mapDtos(List<Player.Avatar> dtos, @Context CycleAvoidingMappingContext context);
}
