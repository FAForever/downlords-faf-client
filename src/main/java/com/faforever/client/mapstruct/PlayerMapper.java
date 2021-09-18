package com.faforever.client.mapstruct;

import com.faforever.client.domain.NameRecordBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.commons.api.dto.NameRecord;
import com.faforever.commons.api.dto.Player;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {LeaderboardMapper.class, AvatarMapper.class, UrlMapper.class}, config = MapperConfiguration.class)
public interface PlayerMapper {
     @Mapping(target = "username", source = "login")
     PlayerBean map(Player dto, @Context CycleAvoidingMappingContext context);

     @Mapping(target = "login", source = "username")
     Player map(PlayerBean bean, @Context CycleAvoidingMappingContext context);

     List<PlayerBean> mapDtos(List<Player> dtos, @Context CycleAvoidingMappingContext context);

     List<Player> mapBeans(List<PlayerBean> beans, @Context CycleAvoidingMappingContext context);

     Set<PlayerBean> mapDtos(Set<Player> dtos, @Context CycleAvoidingMappingContext context);

     Set<Player> mapBeans(Set<PlayerBean> beans, @Context CycleAvoidingMappingContext context);

     NameRecordBean map(NameRecord dto, @Context CycleAvoidingMappingContext context);

     NameRecord map(NameRecordBean bean, @Context CycleAvoidingMappingContext context);

     List<NameRecordBean> mapNameDtos(List<NameRecord> dtos, @Context CycleAvoidingMappingContext context);

     List<NameRecord> mapNameBeans(List<NameRecordBean> beans, @Context CycleAvoidingMappingContext context);

     @Mapping(target = "username", source = "login")
     @Mapping(target = "leaderboardRatings", source = "ratings")
     PlayerBean update(com.faforever.commons.lobby.Player dto, @MappingTarget PlayerBean bean);
}
