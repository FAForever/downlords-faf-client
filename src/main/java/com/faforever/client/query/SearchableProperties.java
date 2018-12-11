package com.faforever.client.query;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Contains mappings of searchable properties (as the API expects it) to their respective i18n key. The reason
 * the i18n keys are not build dynamically is that it makes it impossible for the IDE to detect which key is used where,
 * breaks its refactor capability, and the actual UI text might depend on the context it is used in. Also, this way
 * i18n keys and API keys are nicely decoupled and can therefore be changed independently.
 */
public class SearchableProperties {
  public static final Map<String, String> GAME_PROPERTIES = ImmutableMap.<String, String>builder()
      .put("id", "game.id")
      .put("name", "game.title")
      .put("startTime", "game.startTime")
      .put("endTime", "game.endTime")
      .put("validity", "game.validity")
      .put("victoryCondition", "game.victoryCondition")

      .put("playerStats.faction", "game.player.faction")
      .put("playerStats.team", "game.player.team")
      .put("playerStats.startSpot", "game.player.startSpot")
      .put("playerStats.player.id", "game.player.id")
      .put("playerStats.player.login", "game.player.username")
      .put("playerStats.player.globalRating.rating", "game.player.rating")

      .put("host.id", "game.host.id")
      .put("host.login", "game.host.username")

      .put("featuredMod.displayName", "featuredMod.displayName")
      .put("featuredMod.technicalName", "featuredMod.technicalName")

      .put("mapVersion.description", "map.description")
      .put("mapVersion.maxPlayers", "map.maxPlayers")
      .put("mapVersion.width", "game.map.width")
      .put("mapVersion.height", "game.map.height")
      .put("mapVersion.version", "game.map.version")
      .put("mapVersion.folderName", "game.map.folderName")
      .put("mapVersion.ranked", "game.map.isRanked")
      .put("mapVersion.map.displayName", "game.map.displayName")

      .build();

  public static final Map<String, String> MAP_PROPERTIES = ImmutableMap.<String, String>builder()
      .put("displayName", "map.name")
      .put("author.login", "map.author")

      .put("statistics.plays", "map.playCount")
      .put("statistics.downloads", "map.numberOfDownloads")

      .put("latestVersion.createTime", "map.uploadedDateTime")
      .put("latestVersion.updateTime", "map.updatedDateTime")
      .put("latestVersion.description", "map.description")
      .put("latestVersion.maxPlayers", "map.maxPlayers")
      .put("latestVersion.width", "map.width")
      .put("latestVersion.height", "map.height")
      .put("latestVersion.version", "map.version")
      .put("latestVersion.folderName", "map.folderName")
      .put("latestVersion.ranked", "map.ranked")

      .build();

  public static final Map<String, String> MOD_PROPERTIES = ImmutableMap.<String, String>builder()
      .put("displayName", "mod.displayName")
      .put("author", "mod.author")

      .put("latestVersion.createTime", "mod.uploadedDateTime")
      .put("latestVersion.updateTime", "mod.updatedDateTime")
      .put("latestVersion.description", "mod.description")
      .put("latestVersion.id", "mod.id")
      .put("latestVersion.uid", "mod.uid")
      .put("latestVersion.type", "mod.type")
      .put("latestVersion.ranked", "mod.ranked")
      .put("latestVersion.version", "mod.version")
      .put("latestVersion.filename", "mod.filename")

      .build();

  public static final String NEWEST_MOD_KEY = "latestVersion.createTime";
  public static final String HIGHEST_RATED_MOD_KEY = "latestVersion.reviewsSummary.lowerBound";
}
