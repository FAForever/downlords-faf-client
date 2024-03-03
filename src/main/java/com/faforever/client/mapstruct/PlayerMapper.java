package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.NameRecord;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.commons.api.dto.Player;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;

@Mapper(
    uses = {LeaderboardMapper.class, AvatarMapper.class, UrlMapper.class},
    collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE, config = MapperConfiguration.class
)
public interface PlayerMapper {
  @Mapping(target = "username", source = "login")
  PlayerInfo map(Player dto);

  @InheritInverseConfiguration
  Player map(PlayerInfo bean);

  List<PlayerInfo> mapDtos(List<Player> dtos);

  List<Player> mapBeans(List<PlayerInfo> beans);

  Set<PlayerInfo> mapDtos(Set<Player> dtos);

  Set<Player> mapBeans(Set<PlayerInfo> beans);

  NameRecord map(com.faforever.commons.api.dto.NameRecord dto);

  @InheritInverseConfiguration
  com.faforever.commons.api.dto.NameRecord map(NameRecord bean);

  List<NameRecord> mapNameDtos(List<com.faforever.commons.api.dto.NameRecord> dtos);

  List<com.faforever.commons.api.dto.NameRecord> mapNameBeans(List<NameRecord> beans);

  @Mapping(target = "username", source = "login")
  @Mapping(target = "leaderboardRatings", source = "ratings")
  @Mapping(target = "serverStatus", source = "state")
  PlayerInfo update(com.faforever.commons.lobby.Player dto, @MappingTarget PlayerInfo bean);
}
