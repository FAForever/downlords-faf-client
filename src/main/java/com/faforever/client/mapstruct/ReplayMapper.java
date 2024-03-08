package com.faforever.client.mapstruct;

import com.faforever.client.domain.api.FeaturedMod;
import com.faforever.client.domain.api.GamePlayerStats;
import com.faforever.client.domain.api.LeaderboardRatingJournal;
import com.faforever.client.domain.api.LeagueScoreJournal;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.Replay;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.commons.api.dto.Faction;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.replay.ChatMessage;
import com.faforever.commons.replay.GameOption;
import com.faforever.commons.replay.ReplayDataParser;
import com.faforever.commons.replay.ReplayMetadata;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.InheritInverseConfiguration;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.faforever.client.util.TimeUtil.fromPythonTime;

@Mapper(
    collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE,
    uses = {ModMapper.class, PlayerMapper.class, MapMapper.class, LeaderboardMapper.class, ReviewMapper.class},
    config = MapperConfiguration.class
)
public interface ReplayMapper {

  @Mapping(target = "local", ignore = true)
  @Mapping(target = "title", source = "name")
  @Mapping(target = "teamPlayerStats", source = "dto", qualifiedBy = MapTeamStats.class)
  @Mapping(target = "teams", source = "dto", qualifiedBy = MapTeams.class)
  @Mapping(target = "reviewsSummary", source = "gameReviewsSummary")
  Replay map(Game dto);

  @MapTeams
  default Map<String, List<String>> mapToTeams(Game dto) {
    List<com.faforever.commons.api.dto.GamePlayerStats> playerStats = dto.getPlayerStats();

    if (playerStats == null) {
      return Map.of();
    }

    return playerStats.stream()
                      .filter(gamePlayerStats -> Objects.nonNull(gamePlayerStats.getPlayer()))
                      .collect(Collectors.groupingBy(gamePlayerStats -> String.valueOf(gamePlayerStats.getTeam()),
                                                     Collectors.mapping(
                                                         gamePlayerStats -> gamePlayerStats.getPlayer().getLogin(),
                                                         Collectors.toList())));
  }

  @MapTeamStats
  default Map<String, List<GamePlayerStats>> mapToTeamPlayerStats(Game dto) {
    List<com.faforever.commons.api.dto.GamePlayerStats> playerStats = dto.getPlayerStats();

    if (playerStats == null) {
      return Map.of();
    }

    return playerStats.stream()
                      .collect(Collectors.groupingBy(gamePlayerStats -> String.valueOf(gamePlayerStats.getTeam()),
                                                     Collectors.mapping(gamePlayerStats -> map(gamePlayerStats),
                                                         Collectors.toList())));
  }

  @Mapping(target = "season", source = "leagueSeason")
  @Mapping(target = "divisionBefore", source = "leagueSeasonDivisionSubdivisionBefore")
  @Mapping(target = "divisionAfter", source = "leagueSeasonDivisionSubdivisionAfter")
  LeagueScoreJournal map(com.faforever.commons.api.dto.LeagueScoreJournal dto);

  @InheritInverseConfiguration
  com.faforever.commons.api.dto.LeagueScoreJournal map(LeagueScoreJournal source);

  @Mapping(target = "local", constant = "true")
  @Mapping(target = "id", source = "parser.metadata.uid")
  @Mapping(target = "title", source = "parser.metadata.title")
  @Mapping(target = "replayAvailable", constant = "true")
  @Mapping(target = "featuredMod", source = "featuredMod")
  @Mapping(target = "mapVersion", source = "mapVersion")
  @Mapping(target = "replayFile", source = "replayFile")
  @Mapping(target = "startTime", source = "parser", qualifiedBy = MapStartTime.class)
  @Mapping(target = "endTime", source = "parser", qualifiedBy = MapEndTime.class)
  @Mapping(target = "teamPlayerStats", source = "parser", qualifiedBy = MapTeamStats.class)
  @Mapping(target = "teams", source = "parser", qualifiedBy = MapTeams.class)
  @Mapping(target = "host", ignore = true)
  Replay map(ReplayDataParser parser, Path replayFile, FeaturedMod featuredMod, MapVersion mapVersion);

  Replay.ChatMessage map(ChatMessage chatMessage);

  @Mapping(target = "value", expression = "java(gameOption.getValue().toString())")
  Replay.GameOption map(GameOption gameOption);

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
                 .collect(Collectors.groupingBy(armyInfo -> String.valueOf(((Float) armyInfo.get("Team")).intValue()),
                                                Collectors.mapping(armyInfo -> (String) armyInfo.get("PlayerName"),
                                                                   Collectors.toList())));
  }

  @MapTeamStats
  default HashMap<String, List<GamePlayerStats>> mapTeamStatsFromParser(ReplayDataParser parser) {
    HashMap<String, List<GamePlayerStats>> teams = new HashMap<>();
    parser.getArmies().values().forEach(armyInfo -> {
      if (!(boolean) armyInfo.get("Human")) {
        byte team = ((Float) armyInfo.get("Team")).byteValue();
        String teamString = String.valueOf(team);
        PlayerInfo player = new PlayerInfo();
        player.setId(Integer.parseInt((String) armyInfo.get("OwnerID")));
        player.setUsername((String) armyInfo.get("PlayerName"));
        player.setCountry((String) armyInfo.get("Country"));
        double mean = ((Float) armyInfo.get("MEAN")).doubleValue();
        double deviation = ((Float) armyInfo.get("DEV")).doubleValue();
        LeaderboardRatingJournal ratingJournal = new LeaderboardRatingJournal(null, null, null, mean, deviation, null,
                                                                              null);
        Float factionFloat = (Float) armyInfo.get("Faction");
        Faction faction = Faction.fromFaValue(factionFloat.intValue());
        GamePlayerStats stats = new GamePlayerStats(player, (byte) 0, team, faction, null,
                                                    List.of(ratingJournal));
        teams.computeIfAbsent(teamString, key -> new ArrayList<>()).add(stats);
      }
    });
    return teams;
  }

  @Mapping(target = "name", source = "title")
  @Mapping(target = "playerStats", source = "teamPlayerStats")
  Game map(Replay bean);

  default List<GamePlayerStats> mapToTeamPlayerStats(Map<String, List<GamePlayerStats>> teamPlayerStats) {
    return teamPlayerStats.values().stream().flatMap(Collection::stream).toList();
  }

  GamePlayerStats map(com.faforever.commons.api.dto.GamePlayerStats dto);

  @InheritInverseConfiguration
  com.faforever.commons.api.dto.GamePlayerStats map(GamePlayerStats bean);

  List<com.faforever.commons.api.dto.GamePlayerStats> map(Collection<GamePlayerStats> beans);

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
