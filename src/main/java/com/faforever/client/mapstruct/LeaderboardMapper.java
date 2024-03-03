package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.Division;
import com.faforever.client.domain.api.Leaderboard;
import com.faforever.client.domain.api.LeaderboardEntry;
import com.faforever.client.domain.api.LeaderboardRatingJournal;
import com.faforever.client.domain.api.League;
import com.faforever.client.domain.api.LeagueEntry;
import com.faforever.client.domain.api.LeagueSeason;
import com.faforever.client.domain.api.Subdivision;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.player.LeaderboardRating;
import com.faforever.commons.api.dto.LeagueSeasonDivision;
import com.faforever.commons.api.dto.LeagueSeasonDivisionSubdivision;
import com.faforever.commons.api.dto.LeagueSeasonScore;
import com.faforever.commons.lobby.Player.LeaderboardStats;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(uses = {ReplayMapper.class, PlayerMapper.class, UrlMapper.class}, config = MapperConfiguration.class)
public interface LeaderboardMapper {
  Leaderboard map(com.faforever.commons.api.dto.Leaderboard dto);

  com.faforever.commons.api.dto.Leaderboard map(Leaderboard bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "gamesPlayed", source = "totalGames")
  LeaderboardEntry map(com.faforever.commons.api.dto.LeaderboardEntry dto);

  @Mapping(target = "totalGames", source = "gamesPlayed")
  com.faforever.commons.api.dto.LeaderboardEntry map(LeaderboardEntry bean,
                                                     @Context CycleAvoidingMappingContext context);

  @Mapping(target = "scoreTime", source = "gamePlayerStats.scoreTime")
  LeaderboardRatingJournal map(com.faforever.commons.api.dto.LeaderboardRatingJournal dto);

  com.faforever.commons.api.dto.LeaderboardRatingJournal map(LeaderboardRatingJournal bean,
                                                             @Context CycleAvoidingMappingContext context);

  List<LeaderboardRatingJournal> mapDtoJournals(List<com.faforever.commons.api.dto.LeaderboardRatingJournal> dtos);

  List<com.faforever.commons.api.dto.LeaderboardRatingJournal> mapBeanJournals(List<LeaderboardRatingJournal> beans,
                                                                               @Context CycleAvoidingMappingContext context);

  @Mapping(target = "deviation", source = "rating.deviation")
  @Mapping(target = "mean", source = "rating.mean")
  LeaderboardRating map(LeaderboardStats dto);

  League map(com.faforever.commons.api.dto.League dto);

  com.faforever.commons.api.dto.League map(League bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "leagueLeaderboard", source = "leagueLeaderboard")
  LeagueSeason map(com.faforever.commons.api.dto.LeagueSeason dto);

  @Mapping(target = "leagueLeaderboard", source = "leagueLeaderboard")
  com.faforever.commons.api.dto.LeagueSeason map(LeagueSeason bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "index", source = "divisionIndex")
  Division map(LeagueSeasonDivision dto);

  @Mapping(target = "divisionIndex", source = "index")
  LeagueSeasonDivision map(Division bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "index", source = "subdivisionIndex")
  @Mapping(target = "division", source = "leagueSeasonDivision")
  Subdivision map(LeagueSeasonDivisionSubdivision dto);

  @Mapping(target = "subdivisionIndex", source = "index")
  @Mapping(target = "leagueSeasonDivision", source = "division")
  LeagueSeasonDivisionSubdivision map(Subdivision bean, @Context CycleAvoidingMappingContext context);

  @Mapping(target = "gamesPlayed", source = "source.gameCount")
  @Mapping(target = "subdivision", source = "source.leagueSeasonDivisionSubdivision")
  @Mapping(target = "id", source = "source.id")
  LeagueEntry map(LeagueSeasonScore source, PlayerInfo player, Long rank);

  @Mapping(target = "loginId", source = "player.id")
  @Mapping(target = "gameCount", source = "gamesPlayed")
  @Mapping(target = "leagueSeasonDivisionSubdivision", source = "subdivision")
  LeagueSeasonScore map(LeagueEntry bean, @Context CycleAvoidingMappingContext context);
}
