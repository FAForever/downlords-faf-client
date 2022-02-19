package com.faforever.client.mapstruct;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.LeaderboardRatingJournalBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.util.TimeUtil;
import com.faforever.commons.api.dto.Faction;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.GamePlayerStats;
import com.faforever.commons.replay.ReplayDataParser;
import com.faforever.commons.replay.ReplayMetadata;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.faforever.client.util.TimeUtil.fromPythonTime;

@Mapper(componentModel = "spring", imports = {TimeUtil.class}, uses = {ModMapper.class, PlayerMapper.class, MapMapper.class, LeaderboardMapper.class, ReviewMapper.class}, config = MapperConfiguration.class)
public interface ReplayMapper {
  @Mapping(target="title", source="name")
  @Mapping(target="teamPlayerStats", expression="java(mapToTeamPlayerStats(dto, context))")
  @Mapping(target="teams", expression="java(mapToTeams(dto, context))")
  ReplayBean map(Game dto, @Context CycleAvoidingMappingContext context);

  default Map<String, List<String>> mapToTeams(Game dto, @Context CycleAvoidingMappingContext context) {
    if (dto.getPlayerStats() == null) {
      return Map.of();
    }

    Map<String, List<String>> teams = new HashMap<>();

    dto.getPlayerStats()
        .forEach(gamePlayerStats -> {
          if (gamePlayerStats.getPlayer() == null) {
            return;
          }

          teams.computeIfAbsent(
              String.valueOf(gamePlayerStats.getTeam()),
              key -> new ArrayList<>()
          ).add(gamePlayerStats.getPlayer().getLogin());
        });
    return teams;
  }

  default Map<String, List<GamePlayerStatsBean>> mapToTeamPlayerStats(Game dto, @Context CycleAvoidingMappingContext context) {
    if (dto.getPlayerStats() == null) {
      return Map.of();
    }

    Map<String, List<GamePlayerStatsBean>> teamPlayerStats = new HashMap<>();

    dto.getPlayerStats()
        .forEach(gamePlayerStats -> teamPlayerStats.computeIfAbsent(
            String.valueOf(gamePlayerStats.getTeam()),
            s -> new ArrayList<>()
        ).add(map(gamePlayerStats, context)));
    return teamPlayerStats;
  }

  @Mapping(target = "id", source = "parser.metadata.uid")
  @Mapping(target = "title", source = "parser.metadata.title")
  @Mapping(target = "replayAvailable", constant = "true")
  @Mapping(target = "featuredMod", source = "featuredModBean")
  @Mapping(target = "mapVersion", source = "mapVersionBean")
  @Mapping(target = "replayFile", source = "replayFile")
  @Mapping(target = "startTime", expression = "java(mapStartFromParser(parser))")
  @Mapping(target = "endTime", expression = "java(TimeUtil.fromPythonTime(parser.getMetadata().getGameEnd()))")
  @Mapping(target = "teams", expression = "java(mapTeamsFromParser(parser))")
  @Mapping(target = "teamPlayerStats", expression = "java(mapTeamStatsFromParser(parser))")
  @Mapping(target = "host", ignore = true)
  @Mapping(target = "reviews", ignore = true)
  ReplayBean map(ReplayDataParser parser, Path replayFile, FeaturedModBean featuredModBean, MapVersionBean mapVersionBean);

  default OffsetDateTime mapMetaToStart(ReplayDataParser parser) {
    ReplayMetadata metadata = parser.getMetadata();
    return fromPythonTime(metadata.getGameTime() > 0 ? metadata.getGameTime() : metadata.getLaunchedAt());
  }

  default HashMap<String, List<String>> mapTeamsFromParser(ReplayDataParser parser) {
    HashMap<String, List<String>> teams = new HashMap<>();
    parser.getArmies().values().forEach(armyInfo -> {
      if (!(boolean) armyInfo.get("Human")) {
        String teamString = String.valueOf(((Float) armyInfo.get("Team")).intValue());
        teams.computeIfAbsent(teamString, key -> new ArrayList<>()).add((String) armyInfo.get("PlayerName"));
      }
    });
    return teams;
  }

  default HashMap<String, List<GamePlayerStatsBean>> mapTeamStatsFromParser(ReplayDataParser parser) {
    HashMap<String, List<GamePlayerStatsBean>> teams = new HashMap<>();
    parser.getArmies().values().forEach(armyInfo -> {
      if (!(boolean) armyInfo.get("Human")) {
        int team = ((Float) armyInfo.get("Team")).intValue();
        String teamString = String.valueOf(team);
        PlayerBean player = new PlayerBean();
        player.setId(Integer.parseInt((String) armyInfo.get("OwnerID")));
        player.setUsername((String) armyInfo.get("PlayerName"));
        player.setCountry((String) armyInfo.get("Country"));
        LeaderboardRatingJournalBean ratingJournal = new LeaderboardRatingJournalBean();
        ratingJournal.setMeanBefore(((Float) armyInfo.get("MEAN")).doubleValue());
        ratingJournal.setDeviationBefore(((Float) armyInfo.get("DEV")).doubleValue());
        GamePlayerStatsBean stats = new GamePlayerStatsBean();
        stats.setFaction(Faction.fromFaValue(((Float) armyInfo.get("Faction")).intValue()));
        stats.setTeam(team);
        stats.setLeaderboardRatingJournals(List.of(ratingJournal));
        stats.setPlayer(player);
        teams.computeIfAbsent(teamString, key -> new ArrayList<>()).add(stats);
      }
    });
    return teams;
  }

  @Mapping(target="name", source="title")
  Game map(ReplayBean bean, @Context CycleAvoidingMappingContext context);

  GamePlayerStatsBean map(GamePlayerStats dto, @Context CycleAvoidingMappingContext context);

  GamePlayerStats map(GamePlayerStatsBean bean, @Context CycleAvoidingMappingContext context);
}
