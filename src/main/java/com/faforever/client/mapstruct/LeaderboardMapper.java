package com.faforever.client.mapstruct;

import com.faforever.client.domain.DivisionBean;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardEntryBean;
import com.faforever.client.domain.LeaderboardRatingBean;
import com.faforever.client.domain.LeaderboardRatingJournalBean;
import com.faforever.client.domain.LeagueBean;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.LeagueSeasonBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.commons.api.dto.Leaderboard;
import com.faforever.commons.api.dto.LeaderboardEntry;
import com.faforever.commons.api.dto.LeaderboardRatingJournal;
import com.faforever.commons.api.dto.League;
import com.faforever.commons.api.dto.LeagueSeason;
import com.faforever.commons.api.dto.LeagueSeasonDivision;
import com.faforever.commons.api.dto.LeagueSeasonDivisionSubdivision;
import com.faforever.commons.api.dto.LeagueSeasonScore;
import com.faforever.commons.lobby.Player.LeaderboardStats;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ReplayMapper.class, PlayerMapper.class}, config = MapperConfiguration.class)
public interface LeaderboardMapper {
  LeaderboardBean map(Leaderboard dto, @Context CycleAvoidingMappingContext context);

  Leaderboard map(LeaderboardBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "gamesPlayed", source = "totalGames")
  LeaderboardEntryBean map(LeaderboardEntry dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "totalGames", source = "gamesPlayed")
  LeaderboardEntry map(LeaderboardEntryBean bean, @Context CycleAvoidingMappingContext context);

  LeaderboardRatingJournalBean map(LeaderboardRatingJournal dto, @Context CycleAvoidingMappingContext context);

  LeaderboardRatingJournal map(LeaderboardRatingJournalBean bean, @Context CycleAvoidingMappingContext context);

  List<LeaderboardRatingJournalBean> mapDtoJournals(List<LeaderboardRatingJournal> dtos, @Context CycleAvoidingMappingContext context);

  List<LeaderboardRatingJournal> mapBeanJournals(List<LeaderboardRatingJournalBean> beans, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "deviation", source = "rating.deviation")
  @Mapping(target = "mean", source = "rating.mean")
  LeaderboardRatingBean map(LeaderboardStats dto);

  LeagueBean map(League dto, @Context CycleAvoidingMappingContext context);

  League map(LeagueBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "leaderboard", source = "leagueLeaderboard")
  LeagueSeasonBean map(LeagueSeason dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "leagueLeaderboard", source = "leaderboard")
  LeagueSeason map(LeagueSeasonBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "index", source = "divisionIndex")
  DivisionBean map(LeagueSeasonDivision dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "divisionIndex", source = "index")
  LeagueSeasonDivision map (DivisionBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "leagueSeasonId", expression = "java(Integer.parseInt(dto.getLeagueSeasonDivision().getLeagueSeason().getId()))")
  @Mapping(target = "index", source = "subdivisionIndex")
  @Mapping(target = "division", source = "leagueSeasonDivision")
  SubdivisionBean map(LeagueSeasonDivisionSubdivision dto, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "subdivisionIndex", source = "index")
  @Mapping(target = "leagueSeasonDivision", source = "division")
  LeagueSeasonDivisionSubdivision map(SubdivisionBean bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "gamesPlayed", source = "dto.gameCount")
  @Mapping(target = "subdivision", source = "dto.leagueSeasonDivisionSubdivision")
  @Mapping(target = "id", source = "dto.id")
  @Mapping(target = "createTime", source = "dto.createTime")
  @Mapping(target = "updateTime", source = "dto.updateTime")
  LeagueEntryBean map(LeagueSeasonScore dto, PlayerBean player, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "loginId", source = "player.id")
  @Mapping(target = "gameCount", source = "gamesPlayed")
  @Mapping(target = "leagueSeasonDivisionSubdivision", source = "subdivision")
  LeagueSeasonScore map(LeagueEntryBean bean, @Context CycleAvoidingMappingContext context);
}
