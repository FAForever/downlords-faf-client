package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.Clan;
import com.faforever.commons.api.dto.ClanMembership;
import com.faforever.commons.api.dto.Player;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {PlayerMapper.class}, config = MapperConfiguration.class)
public interface ClanMapper {

  @Mapping(target = "members", source = "memberships")
  Clan map(com.faforever.commons.api.dto.Clan dto);

  @Mapping(target = "memberships", ignore = true)
  @InheritInverseConfiguration
  com.faforever.commons.api.dto.Clan map(Clan bean);

  default Player map(ClanMembership dto) {
    return dto.getPlayer();
  }
}