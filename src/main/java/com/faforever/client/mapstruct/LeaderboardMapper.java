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
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(uses = {ReplayMapper.class, PlayerMapper.class, UrlMapper.class}, config = MapperConfiguration.class)
public interface LeaderboardMapper {
  Leaderboard map(com.faforever.commons.api.dto.Leaderboard dto);

  @InheritInverseConfiguration
  com.faforever.commons.api.dto.Leaderboard map(Leaderboard bean);

  @Mapping(target = "gamesPlayed", source = "totalGames")
  LeaderboardEntry map(com.faforever.commons.api.dto.LeaderboardEntry dto);

  @InheritInverseConfiguration
  com.faforever.commons.api.dto.LeaderboardEntry map(LeaderboardEntry bean);

  @Mapping(target = "scoreTime", source = "gamePlayerStats.scoreTime")
  LeaderboardRatingJournal map(com.faforever.commons.api.dto.LeaderboardRatingJournal dto);

  @InheritInverseConfiguration
  com.faforever.commons.api.dto.LeaderboardRatingJournal map(LeaderboardRatingJournal bean);

  List<LeaderboardRatingJournal> mapDtoJournals(List<com.faforever.commons.api.dto.LeaderboardRatingJournal> dtos);

  List<com.faforever.commons.api.dto.LeaderboardRatingJournal> mapBeanJournals(List<LeaderboardRatingJournal> beans);

  @Mapping(target = "deviation", source = "rating.deviation")
  @Mapping(target = "mean", source = "rating.mean")
  LeaderboardRating map(LeaderboardStats dto);

  League map(com.faforever.commons.api.dto.League dto);

  @InheritInverseConfiguration
  com.faforever.commons.api.dto.League map(League bean);

  LeagueSeason map(com.faforever.commons.api.dto.LeagueSeason dto);

  @InheritInverseConfiguration
  com.faforever.commons.api.dto.LeagueSeason map(LeagueSeason bean);

  @Mapping(target = "index", source = "divisionIndex")
  Division map(LeagueSeasonDivision dto);

  @InheritInverseConfiguration
  LeagueSeasonDivision map(Division bean);

  @Mapping(target = "index", source = "subdivisionIndex")
  @Mapping(target = "division", source = "leagueSeasonDivision")
  Subdivision map(LeagueSeasonDivisionSubdivision dto);

  @InheritInverseConfiguration
  LeagueSeasonDivisionSubdivision map(Subdivision bean);

  @Mapping(target = "gamesPlayed", source = "source.gameCount")
  @Mapping(target = "subdivision", source = "source.leagueSeasonDivisionSubdivision")
  @Mapping(target = "id", source = "source.id")
  LeagueEntry map(LeagueSeasonScore source, PlayerInfo player, Long rank);

  @InheritInverseConfiguration
  @Mapping(target = "loginId", source = "player.id")
  @Mapping(target = "gameCount", source = "gamesPlayed")
  @Mapping(target = "leagueSeasonDivisionSubdivision", source = "subdivision")
  LeagueSeasonScore map(LeagueEntry bean);
}
