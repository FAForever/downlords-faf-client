package com.faforever.client.mapstruct;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.util.TimeUtil;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.GamePlayerStats;
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

@Mapper(componentModel = "spring", imports = {TimeUtil.class}, uses = {ModMapper.class, PlayerMapper.class, MapMapper.class, LeaderboardMapper.class, ReviewMapper.class})
public interface ReplayMapper {
  @Mapping(target="title", source="name")
  @Mapping(target="teamPlayerStats", expression="java(mapToTeamPlayerStats(dto, new CycleAvoidingMappingContext()))")
  @Mapping(target="teams", expression="java(mapToTeams(dto, new CycleAvoidingMappingContext()))")
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

  @Mapping(target = "id", source = "metadata.uid")
  @Mapping(target = "title", source = "metadata.title")
  @Mapping(target = "replayAvailable", constant = "true")
  @Mapping(target = "featuredMod", source = "featuredModBean")
  @Mapping(target = "mapVersion", source = "mapVersionBean")
  @Mapping(target = "replayFile", source = "replayFile")
  @Mapping(target = "startTime", expression = "java(TimeUtil.fromPythonTime(metadata.getGameTime() > 0 ? metadata.getGameTime() : metadata.getLaunchedAt()))")
  @Mapping(target = "endTime", expression = "java(TimeUtil.fromPythonTime(metadata.getGameEnd()))")
  @Mapping(target = "host", ignore = true)
  @Mapping(target = "reviews", ignore = true)
  ReplayBean map(ReplayMetadata metadata, Path replayFile, FeaturedModBean featuredModBean, MapVersionBean mapVersionBean, @Context CycleAvoidingMappingContext context);

  default OffsetDateTime mapMetaToStart(ReplayMetadata metadata) {
    return fromPythonTime(metadata.getGameTime() > 0 ? metadata.getGameTime() : metadata.getLaunchedAt());
  }

  @Mapping(target="name", source="title")
  Game map(ReplayBean bean, @Context CycleAvoidingMappingContext context);

  GamePlayerStatsBean map(GamePlayerStats dto, @Context CycleAvoidingMappingContext context);

  GamePlayerStats map(GamePlayerStatsBean bean, @Context CycleAvoidingMappingContext context);
}
