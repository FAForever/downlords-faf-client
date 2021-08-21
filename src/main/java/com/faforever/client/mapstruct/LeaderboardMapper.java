package com.faforever.client.mapstruct;

import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardEntryBean;
import com.faforever.client.domain.LeaderboardRatingBean;
import com.faforever.client.domain.LeaderboardRatingJournalBean;
import com.faforever.commons.api.dto.Leaderboard;
import com.faforever.commons.api.dto.LeaderboardEntry;
import com.faforever.commons.api.dto.LeaderboardRatingJournal;
import com.faforever.commons.lobby.Player.LeaderboardStats;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ReplayMapper.class, PlayerMapper.class})
public interface LeaderboardMapper {
  LeaderboardBean map(Leaderboard dto, @Context CycleAvoidingMappingContext context);

  Leaderboard map(LeaderboardBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "gamesPlayed", source = "totalGames")
  LeaderboardEntryBean map(LeaderboardEntry dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "totalGames", source = "gamesPlayed")
  LeaderboardEntry map(LeaderboardEntryBean bean, @Context CycleAvoidingMappingContext context);

  List<LeaderboardEntryBean> mapDtoEntries(List<LeaderboardEntry> dtos, @Context CycleAvoidingMappingContext context);

  List<LeaderboardEntry> mapBeanEntries(List<LeaderboardEntryBean> beans, @Context CycleAvoidingMappingContext context);

  LeaderboardRatingJournalBean map(LeaderboardRatingJournal dto, @Context CycleAvoidingMappingContext context);

  LeaderboardRatingJournal map(LeaderboardRatingJournalBean bean, @Context CycleAvoidingMappingContext context);

  List<LeaderboardRatingJournalBean> mapDtoJournals(List<LeaderboardRatingJournal> dtos, @Context CycleAvoidingMappingContext context);

  List<LeaderboardRatingJournal> mapBeanJournals(List<LeaderboardRatingJournalBean> beans, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "deviation", source = "rating.deviation")
  @Mapping(target = "mean", source = "rating.mean")
  LeaderboardRatingBean map(LeaderboardStats dto);
}
