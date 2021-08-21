package com.faforever.client.mapstruct;

import com.faforever.client.domain.AvatarBean;
import com.faforever.commons.api.dto.Avatar;
import com.faforever.commons.lobby.Player;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UrlMapper.class})
public interface AvatarMapper {

  @Mapping(target = "description", source = "tooltip")
  AvatarBean map(Avatar dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "tooltip", source = "description")
  Avatar map(AvatarBean bean, @Context CycleAvoidingMappingContext context);

  AvatarBean map(Player.Avatar dto, @Context CycleAvoidingMappingContext context);

  List<AvatarBean> mapDtos(List<Player.Avatar> dtos, @Context CycleAvoidingMappingContext context);
}
