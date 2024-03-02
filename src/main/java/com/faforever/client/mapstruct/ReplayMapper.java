package com.faforever.client.mapstruct;

import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.LeaderboardRatingJournalBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.commons.api.dto.Faction;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.GamePlayerStats;
import com.faforever.commons.replay.ChatMessage;
import com.faforever.commons.replay.GameOption;
import com.faforever.commons.replay.ReplayDataParser;
import com.faforever.commons.replay.ReplayMetadata;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.faforever.client.util.TimeUtil.fromPythonTime;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE, uses = {ModMapper.class, PlayerMapper.class, MapMapper.class, LeaderboardMapper.class, ReviewMapper.class}, config = MapperConfiguration.class)
public interface ReplayMapper {

  @Mapping(target = "local", ignore = true)
  @Mapping(target = "title", source = "name")
  @Mapping(target = "teamPlayerStats", source = "dto", qualifiedBy = MapTeamStats.class)
  @Mapping(target = "teams", source = "dto", qualifiedBy = MapTeams.class)
  ReplayBean map(Game dto, @Context CycleAvoidingMappingContext context);

  @MapTeams
  default Map<String, List<String>> mapToTeams(Game dto, @Context CycleAvoidingMappingContext context) {
    List<GamePlayerStats> playerStats = dto.getPlayerStats();

    if (playerStats == null) {
      return Map.of();
    }

    return playerStats.stream()
        .filter(gamePlayerStats -> Objects.nonNull(gamePlayerStats.getPlayer()))
        .collect(Collectors.groupingBy(gamePlayerStats -> String.valueOf(gamePlayerStats.getTeam()), Collectors.mapping(gamePlayerStats -> gamePlayerStats.getPlayer()
            .getLogin(), Collectors.toList())));
  }

  @MapTeamStats
  default Map<String, List<GamePlayerStatsBean>> mapToTeamPlayerStats(Game dto,
                                                                      @Context CycleAvoidingMappingContext context) {
    List<GamePlayerStats> playerStats = dto.getPlayerStats();

    if (playerStats == null) {
      return Map.of();
    }

    return playerStats.stream()
        .collect(Collectors.groupingBy(gamePlayerStats -> String.valueOf(gamePlayerStats.getTeam()), Collectors.mapping(gamePlayerStats -> map(gamePlayerStats, context), Collectors.toList())));
  }

  @Mapping(target = "local", constant = "true")
  @Mapping(target = "id", source = "parser.metadata.uid")
  @Mapping(target = "title", source = "parser.metadata.title")
  @Mapping(target = "replayAvailable", constant = "true")
  @Mapping(target = "featuredMod", source = "featuredModBean")
  @Mapping(target = "mapVersion", source = "mapVersionBean")
  @Mapping(target = "replayFile", source = "replayFile")
  @Mapping(target = "startTime", source = "parser", qualifiedBy = MapStartTime.class)
  @Mapping(target = "endTime", source = "parser", qualifiedBy = MapEndTime.class)
  @Mapping(target = "teamPlayerStats", source = "parser", qualifiedBy = MapTeamStats.class)
  @Mapping(target = "teams", source = "parser", qualifiedBy = MapTeams.class)
  @Mapping(target = "host", ignore = true)
  ReplayBean map(ReplayDataParser parser, Path replayFile, FeaturedModBean featuredModBean,
                 MapVersionBean mapVersionBean);

  ReplayBean.ChatMessage map(ChatMessage chatMessage);

  @Mapping(target = "value", expression = "java(gameOption.getValue().toString())")
  ReplayBean.GameOption map(GameOption gameOption);

  @MapStartTime
  default OffsetDateTime mapStartFromParser(ReplayDataParser parser) {
    ReplayMetadata metadata = parser.getMetadata();
    return fromPythonTime(metadata.getGameTime() > 0 ? metadata.getGameTime() : metadata.getLaunchedAt());
  }

  @MapEndTime
  default OffsetDateTime mapEndFromParser(ReplayDataParser parser) {
    return fromPythonTime(parser.getMetadata().getGameEnd());
  }

  @MapTeams
  default Map<String, List<String>> mapTeamsFromParser(ReplayDataParser parser) {
    return parser.getArmies()
        .values()
        .stream()
        .filter(armyInfo -> !((boolean) armyInfo.get("Human")))
        .collect(Collectors.groupingBy(armyInfo -> String.valueOf(((Float) armyInfo.get("Team")).intValue()), Collectors.mapping(armyInfo -> (String) armyInfo.get("PlayerName"), Collectors.toList())));
  }

  @MapTeamStats
  default HashMap<String, List<GamePlayerStatsBean>> mapTeamStatsFromParser(ReplayDataParser parser) {
    HashMap<String, List<GamePlayerStatsBean>> teams = new HashMap<>();
    parser.getArmies().values().forEach(armyInfo -> {
      if (!(boolean) armyInfo.get("Human")) {
        byte team = ((Float) armyInfo.get("Team")).byteValue();
        String teamString = String.valueOf(team);
        PlayerBean player = new PlayerBean();
        player.setId(Integer.parseInt((String) armyInfo.get("OwnerID")));
        player.setUsername((String) armyInfo.get("PlayerName"));
        player.setCountry((String) armyInfo.get("Country"));
        double mean = ((Float) armyInfo.get("MEAN")).doubleValue();
        double deviation = ((Float) armyInfo.get("DEV")).doubleValue();
        LeaderboardRatingJournalBean ratingJournal = new LeaderboardRatingJournalBean(null, null, null, mean, deviation,
                                                                                      null, null);
        Float factionFloat = (Float) armyInfo.get("Faction");
        Faction faction = Faction.fromFaValue(factionFloat.intValue());
        GamePlayerStatsBean stats = new GamePlayerStatsBean(player, (byte) 0, team, faction, null, null,
                                                            List.of(ratingJournal));
        teams.computeIfAbsent(teamString, key -> new ArrayList<>()).add(stats);
      }
    });
    return teams;
  }

  @Mapping(target = "name", source = "title")
  Game map(ReplayBean bean, @Context CycleAvoidingMappingContext context);

  GamePlayerStatsBean map(GamePlayerStats dto, @Context CycleAvoidingMappingContext context);

  GamePlayerStats map(GamePlayerStatsBean bean, @Context CycleAvoidingMappingContext context);

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.CLASS)
  @interface MapTeams {}

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.CLASS)
  @interface MapTeamStats {}

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.CLASS)
  @interface MapStartTime {}

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.CLASS)
  @interface MapEndTime {}
}
