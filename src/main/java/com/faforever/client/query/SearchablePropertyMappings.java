package com.faforever.client.query;

import com.google.common.collect.ImmutableMap;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * Contains mappings of searchable properties (as the API expects it) to their respective i18n key and if they are considered for sorting the result list. The reason
 * the i18n keys are not build dynamically is that it makes it impossible for the IDE to detect which key is used where,
 * breaks its refactor capability, and the actual UI text might depend on the context it is used in. Also, this way
 * i18n keys and API keys are nicely decoupled and can therefore be changed independently.
 */
public class SearchablePropertyMappings {
  public static final String NEWEST_MOD_KEY = "latestVersion.createTime";
  public static final String HIGHEST_RATED_MOD_KEY = "reviewsSummary.lowerBound";

  public static final Map<String, Property> GAME_PROPERTY_MAPPING = ImmutableMap.<String, Property>builder()
      .put("playerStats.player.login", new Property("game.player.username", false))
      .put("playerStats.player.globalRating.rating", new Property("game.player.globalRating", false))
      .put("playerStats.player.ladder1v1Rating.rating", new Property("game.player.ladderRating", false))
      .put("featuredMod.technicalName", new Property("featuredMod.technicalName", false))
      .put("mapVersion.map.displayName", new Property("game.map.displayName", false))
      .put("playerStats.faction", new Property("game.player.faction", false))
      .put("playerStats.startSpot", new Property("game.player.startSpot", false))
      .put("mapVersion.maxPlayers", new Property("map.maxPlayers", false))
      .put("mapVersion.ranked", new Property("game.map.isRanked", false))
      .put("id", new Property("game.id", true))
      .put("playerStats.player.id", new Property("game.player.id", false))
      .put("name", new Property("game.title", true))
      .put("startTime", new Property("game.startTime", true))
      .put("endTime", new Property("game.endTime", true))
      .put("validity", new Property("game.validity", true))
      .put("victoryCondition", new Property("game.victoryCondition", true))
      .put("playerStats.team", new Property("game.player.team", false))
      .put("host.login", new Property("game.host.username", false))
      .put("host.id", new Property("game.host.id", false))
      .put("featuredMod.displayName", new Property("featuredMod.displayName", false))
      .put("mapVersion.description", new Property("map.description", false))
      .put("mapVersion.width", new Property("map.widthPixels", false))
      .put("mapVersion.height", new Property("map.heightPixels", false))
      .put("mapVersion.folderName", new Property("game.map.folderName", false))
      .put("mapVersion.map.author.login", new Property("game.map.author", false))

      .build();

  public static final Map<String, Property> MAP_PROPERTY_MAPPING = ImmutableMap.<String, Property>builder()
      .put("displayName", new Property("map.name", true))
      .put("author.login", new Property("map.author", false))

      .put("statistics.plays", new Property("map.playCount", false))
      .put("statistics.downloads", new Property("map.numberOfDownloads", false))

      .put("latestVersion.createTime", new Property("map.uploadedDateTime", false))
      .put("latestVersion.updateTime", new Property("map.updatedDateTime", false))
      .put("latestVersion.description", new Property("map.description", false))
      .put("latestVersion.maxPlayers", new Property("map.maxPlayers", false))
      .put("latestVersion.width", new Property("map.widthPixels", false))
      .put("latestVersion.height", new Property("map.heightPixels", false))
      .put("latestVersion.folderName", new Property("map.folderName", false))
      .put("latestVersion.ranked", new Property("map.ranked", false))
      .put("latestVersion.id", new Property("map.versionId", false))

      .build();

  public static final Map<String, Property> MOD_PROPERTY_MAPPING = ImmutableMap.<String, Property>builder()
      .put("displayName", new Property("mod.displayName", true))
      .put("author", new Property("mod.author", false))
      .put("uploader.login", new Property("mod.uploader.login", false))
      .put("latestVersion.createTime", new Property("mod.uploadedDateTime", false))
      .put("latestVersion.updateTime", new Property("mod.updatedDateTime", false))
      .put("latestVersion.description", new Property("mod.description", false))
      .put("latestVersion.id", new Property("mod.id", false))
      .put("latestVersion.uid", new Property("mod.uid", false))
      .put("latestVersion.type", new Property("mod.type", false))
      .put("latestVersion.ranked", new Property("mod.ranked", false))
      .put("latestVersion.filename", new Property("mod.filename", false))

      .build();

  @Data
  @EqualsAndHashCode(of = "i18nKey")
  public static class Property {
    private final String i18nKey;
    private final boolean sortable;
  }
}
