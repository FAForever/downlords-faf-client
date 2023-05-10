package com.faforever.client.query;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Contains mappings of searchable properties (as the API expects it) to their respective i18n key and if they are
 * considered for sorting the result list. The reason the i18n keys are not build dynamically is that it makes it
 * impossible for the IDE to detect which key is used where, breaks its refactor capability, and the actual UI text
 * might depend on the context it is used in. Also, this way i18n keys and API keys are nicely decoupled and can
 * therefore be changed independently.
 */
public class SearchablePropertyMappings {
  public static final Map<String, Property> GAME_PROPERTY_MAPPING = ImmutableMap.<String, Property>builder()
      .put("playerStats.player.login", new Property("game.player.username", true))
      .put("featuredMod.technicalName", new Property("featuredMod.technicalName", true))
      .put("mapVersion.map.displayName", new Property("game.map.displayName", true))
      .put("playerStats.faction", new Property("game.player.faction", true))
      .put("playerStats.startSpot", new Property("game.player.startSpot", true))
      .put("mapVersion.maxPlayers", new Property("map.maxPlayers", true))
      .put("mapVersion.ranked", new Property("game.map.isRanked", true))
      .put("id", new Property("game.id", true))
      .put("playerStats.player.id", new Property("game.player.id", true))
      .put("name", new Property("game.title", true))
      .put("replayTicks", new Property("game.replayTicks", true))
      .put("startTime", new Property("game.startTime", true))
      .put("endTime", new Property("game.endTime", true))
      .put("validity", new Property("game.validity", true))
      .put("victoryCondition", new Property("game.victoryCondition", true))
      .put("playerStats.team", new Property("game.player.team", true))
      .put("host.login", new Property("game.host.username", true))
      .put("host.id", new Property("game.host.id", true))
      .put("featuredMod.displayName", new Property("featuredMod.displayName", true))
      .put("mapVersion.description", new Property("map.description", true))
      .put("mapVersion.width", new Property("map.widthPixels", true))
      .put("mapVersion.height", new Property("map.heightPixels", true))
      .put("mapVersion.folderName", new Property("game.map.folderName", true))
      .put("mapVersion.map.author.login", new Property("game.map.author", true))

      .build();

  public static final Map<String, Property> MAP_PROPERTY_MAPPING = ImmutableMap.<String, Property>builder()
      .put("displayName", new Property("map.name", true))
      .put("author.login", new Property("map.author", true))

      .put("gamesPlayed", new Property("map.playCount", true))

      .put("latestVersion.createTime", new Property("map.uploadedDateTime", true))
      .put("latestVersion.updateTime", new Property("map.updatedDateTime", true))
      .put("latestVersion.description", new Property("map.description", true))
      .put("latestVersion.maxPlayers", new Property("map.maxPlayers", true))
      .put("latestVersion.width", new Property("map.widthPixels", true))
      .put("latestVersion.height", new Property("map.heightPixels", true))
      .put("latestVersion.folderName", new Property("map.folderName", true))
      .put("latestVersion.ranked", new Property("map.ranked", true))
      .put("latestVersion.id", new Property("map.versionId", true))

      .build();

  public static final Map<String, Property> MOD_PROPERTY_MAPPING = ImmutableMap.<String, Property>builder()
      .put("displayName", new Property("mod.displayName", true))
      .put("author", new Property("mod.author", true))
      .put("uploader.login", new Property("mod.uploader.login", true))
      .put("latestVersion.createTime", new Property("mod.uploadedDateTime", true))
      .put("latestVersion.updateTime", new Property("mod.updatedDateTime", true))
      .put("latestVersion.description", new Property("mod.description", true))
      .put("latestVersion.id", new Property("mod.id", true))
      .put("latestVersion.uid", new Property("mod.uid", true))
      .put("latestVersion.type", new Property("mod.type", true))
      .put("latestVersion.ranked", new Property("mod.ranked", true))
      .put("latestVersion.filename", new Property("mod.filename", true))

      .build();

  public record Property(String i18nKey, boolean sortable) {}
}
