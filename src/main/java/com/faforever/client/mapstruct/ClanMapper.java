package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.Clan;
import com.faforever.commons.api.dto.ClanMembership;
import com.faforever.commons.api.dto.Player;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Mapper(uses = {PlayerMapper.class}, config = MapperConfiguration.class)
public abstract class ClanMapper {

  @Autowired
  private PlayerMapper playerMapper;

  @Mapping(target = "members", source = "memberships")
  public abstract Clan map(com.faforever.commons.api.dto.Clan dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "memberships", source = "bean")
  public abstract com.faforever.commons.api.dto.Clan map(Clan bean, @Context CycleAvoidingMappingContext context);

  public Player map(ClanMembership dto, @Context CycleAvoidingMappingContext context) {
    return dto.getPlayer();
  }

  public List<ClanMembership> mapMembership(Clan bean, @Context CycleAvoidingMappingContext context) {
    com.faforever.commons.api.dto.Clan clan = map(bean, context);
    List<ClanMembership> memberships = new ArrayList<>();
    bean.members()
        .forEach(playerBean -> memberships.add(
            new ClanMembership().setClan(clan).setPlayer(playerMapper.map(playerBean, context))));
    return memberships;
  }
}